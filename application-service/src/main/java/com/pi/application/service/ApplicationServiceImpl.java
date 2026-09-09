package com.pi.application.service;

import com.pi.application.client.JobServiceClient;
import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.ApplicationStatsResponse;
import com.pi.application.dto.ApplicationStatusHistoryResponse;
import com.pi.application.dto.CreateApplicationRequest;
import com.pi.application.dto.InterviewEligibilityResponse;
import com.pi.application.dto.UpdateApplicationStatusRequest;
import com.pi.application.entity.ApplicationStatusHistory;
import com.pi.application.entity.JobApplication;
import com.pi.application.enums.ApplicationStatus;
import com.pi.application.exception.ApplicationNotFoundException;
import com.pi.application.exception.DuplicateApplicationException;
import com.pi.application.exception.InvalidApplicationStateException;
import com.pi.application.mapper.ApplicationMapper;
import com.pi.application.repository.ApplicationRepository;
import com.pi.application.repository.ApplicationStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobServiceClient jobServiceClient;
    private final ApplicationMapper mapper;

    @Override
    @Transactional
    public ApplicationResponse applyForJob(CreateApplicationRequest request, UUID candidateId) {
        UUID jobId = request.jobId();

        // 1. Verify Job is open for applications
        if (!jobServiceClient.isJobOpenForApplications(jobId)) {
            throw new IllegalArgumentException("Job opening " + jobId + " is not currently accepting applications");
        }

        // 2. Prevent duplicate applications
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new DuplicateApplicationException(jobId, candidateId);
        }

        Instant now = Instant.now();
        UUID applicationId = UUID.randomUUID();

        // 3. Create initial application in APPLIED status
        JobApplication application = JobApplication.builder()
                .id(applicationId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.APPLIED)
                .resumeUrl(request.resumeUrl() != null ? request.resumeUrl().trim() : null)
                .coverNote(request.coverNote() != null ? request.coverNote().trim() : null)
                .appliedAt(now)
                .updatedAt(now)
                .build();

        applicationRepository.save(application);

        // 4. Record initial status history audit entry
        recordStatusHistory(applicationId, null, ApplicationStatus.APPLIED, candidateId, "Application submitted by candidate");

        log.info("Application created successfully: {} for job {} by candidate {}", applicationId, jobId, candidateId);
        return mapper.toResponse(application);
    }

    @Override
    public ApplicationResponse getApplicationById(UUID id, UUID requestingUser, boolean isAdmin) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        if (!isAdmin && !app.getCandidateId().equals(requestingUser)) {
            throw new ApplicationNotFoundException(id);
        }

        return mapper.toResponse(app);
    }

    @Override
    public Page<ApplicationResponse> getCandidateApplications(UUID candidateId, Pageable pageable) {
        return applicationRepository.findByCandidateId(candidateId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ApplicationResponse withdrawApplication(UUID applicationId, UUID candidateId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (!app.getCandidateId().equals(candidateId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        // Candidates can only withdraw before being shortlisted (APPLIED or UNDER_REVIEW)
        if (app.getStatus() != ApplicationStatus.APPLIED && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new InvalidApplicationStateException("Cannot withdraw application from status: " + app.getStatus());
        }

        ApplicationStatus previousStatus = app.getStatus();
        app.setStatus(ApplicationStatus.WITHDRAWN);
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);

        recordStatusHistory(applicationId, previousStatus, ApplicationStatus.WITHDRAWN, candidateId, "Application withdrawn by candidate");

        log.info("Application {} withdrawn by candidate {}", applicationId, candidateId);
        return mapper.toResponse(app);
    }

    @Override
    public Page<ApplicationResponse> getAdminApplications(UUID jobId, ApplicationStatus status, Pageable pageable) {
        if (jobId != null && status != null) {
            return applicationRepository.findByJobIdAndStatus(jobId, status, pageable).map(mapper::toResponse);
        } else if (jobId != null) {
            return applicationRepository.findByJobId(jobId, pageable).map(mapper::toResponse);
        } else if (status != null) {
            return applicationRepository.findByStatus(status, pageable).map(mapper::toResponse);
        } else {
            return applicationRepository.findAll(pageable).map(mapper::toResponse);
        }
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplicationStatus(
            UUID applicationId, UpdateApplicationStatusRequest request, UUID adminId) {

        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        ApplicationStatus currentStatus = app.getStatus();
        ApplicationStatus targetStatus = request.status();

        validateStateTransition(currentStatus, targetStatus);

        app.setStatus(targetStatus);
        if (targetStatus == ApplicationStatus.REJECTED && request.rejectionReason() != null) {
            app.setRejectionReason(request.rejectionReason().trim());
        }
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);

        recordStatusHistory(applicationId, currentStatus, targetStatus, adminId, request.comments());

        log.info("Application {} transitioned from {} to {} by admin {}", applicationId, currentStatus, targetStatus, adminId);
        return mapper.toResponse(app);
    }

    @Override
    public InterviewEligibilityResponse checkInterviewEligibility(UUID applicationId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        boolean eligible = app.getStatus() == ApplicationStatus.SHORTLISTED;
        String message = eligible
                ? "Candidate is shortlisted and eligible for interview scheduling"
                : "Candidate application is not shortlisted (current status: " + app.getStatus() + ")";

        return new InterviewEligibilityResponse(
                app.getId(),
                app.getCandidateId(),
                app.getJobId(),
                eligible,
                app.getStatus(),
                message
        );
    }

    @Override
    @Transactional
    public void markInterviewScheduled(UUID applicationId, UUID changedBy) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (app.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw new InvalidApplicationStateException("Cannot schedule interview for application in status: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);

        recordStatusHistory(applicationId, ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED, changedBy, "Interview scheduled");
        log.info("Application {} marked as INTERVIEW_SCHEDULED", applicationId);
    }

    @Override
    public List<ApplicationStatusHistoryResponse> getApplicationHistory(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        return historyRepository.findByApplicationIdOrderByChangedAtAsc(applicationId).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    @Override
    public ApplicationStatsResponse getStats() {
        long total = applicationRepository.count();
        long applied = applicationRepository.countByStatus(ApplicationStatus.APPLIED);
        long underReview = applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW);
        long shortlisted = applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED);
        long interviewScheduled = applicationRepository.countByStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        long hired = applicationRepository.countByStatus(ApplicationStatus.HIRED);
        long rejected = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        long withdrawn = applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN);

        return new ApplicationStatsResponse(total, applied, underReview, shortlisted, interviewScheduled, hired, rejected, withdrawn);
    }

    private void validateStateTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == to) {
            return;
        }

        // Terminal states
        if (from == ApplicationStatus.HIRED || from == ApplicationStatus.REJECTED || from == ApplicationStatus.WITHDRAWN) {
            throw new InvalidApplicationStateException("Cannot transition application from terminal status: " + from);
        }

        // Rejection is allowed from any active non-terminal status
        if (to == ApplicationStatus.REJECTED) {
            return;
        }

        boolean valid = switch (from) {
            case APPLIED -> to == ApplicationStatus.UNDER_REVIEW;
            case UNDER_REVIEW -> to == ApplicationStatus.SHORTLISTED;
            case SHORTLISTED -> to == ApplicationStatus.INTERVIEW_SCHEDULED;
            case INTERVIEW_SCHEDULED -> to == ApplicationStatus.HIRED;
            default -> false;
        };

        if (!valid) {
            throw new InvalidApplicationStateException("Illegal application state transition from " + from + " to " + to);
        }
    }

    private void recordStatusHistory(
            UUID applicationId, ApplicationStatus from, ApplicationStatus to, UUID changedBy, String comments) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .comments(comments)
                .changedAt(Instant.now())
                .build();

        historyRepository.save(history);
    }
}
