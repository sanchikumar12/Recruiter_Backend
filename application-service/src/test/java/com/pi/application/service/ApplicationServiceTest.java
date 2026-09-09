package com.pi.application.service;

import com.pi.application.client.JobServiceClient;
import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.ApplicationStatsResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    @Mock
    private JobServiceClient jobServiceClient;

    private ApplicationMapper mapper;
    private ApplicationServiceImpl applicationService;

    private UUID candidateId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        mapper = new ApplicationMapper();
        applicationService = new ApplicationServiceImpl(applicationRepository, historyRepository, jobServiceClient, mapper);
        candidateId = UUID.randomUUID();
        jobId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully submit application in APPLIED status")
    void shouldApplySuccessfully() {
        CreateApplicationRequest request = new CreateApplicationRequest(jobId, "https://storage.366pi.com/resumes/c1.pdf", "Passionate about high-scale systems");

        when(jobServiceClient.isJobOpenForApplications(jobId)).thenReturn(true);
        when(applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(false);
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = applicationService.applyForJob(request, candidateId);

        assertThat(response).isNotNull();
        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.candidateId()).isEqualTo(candidateId);
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);

        verify(applicationRepository).save(any(JobApplication.class));
        verify(historyRepository).save(any(ApplicationStatusHistory.class));
    }

    @Test
    @DisplayName("Should reject duplicate application for same job and candidate")
    void shouldPreventDuplicateApplication() {
        CreateApplicationRequest request = new CreateApplicationRequest(jobId, null, null);

        when(jobServiceClient.isJobOpenForApplications(jobId)).thenReturn(true);
        when(applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyForJob(request, candidateId))
                .isInstanceOf(DuplicateApplicationException.class);
    }

    @Test
    @DisplayName("Should fail application if job is not open")
    void shouldFailWhenJobNotOpen() {
        CreateApplicationRequest request = new CreateApplicationRequest(jobId, null, null);

        when(jobServiceClient.isJobOpenForApplications(jobId)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.applyForJob(request, candidateId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not currently accepting applications");
    }

    @Test
    @DisplayName("Should allow candidate to withdraw application when in APPLIED or UNDER_REVIEW status")
    void shouldWithdrawApplication() {
        UUID appId = UUID.randomUUID();
        JobApplication app = JobApplication.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.APPLIED)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = applicationService.withdrawApplication(appId, candidateId);

        assertThat(response.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        verify(historyRepository).save(any(ApplicationStatusHistory.class));
    }

    @Test
    @DisplayName("Should reject withdrawal if application is already SHORTLISTED")
    void shouldFailWithdrawalWhenShortlisted() {
        UUID appId = UUID.randomUUID();
        JobApplication app = JobApplication.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.withdrawApplication(appId, candidateId))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessageContaining("Cannot withdraw application from status: SHORTLISTED");
    }

    @Test
    @DisplayName("Should transition APPLIED -> UNDER_REVIEW -> SHORTLISTED -> INTERVIEW_SCHEDULED -> HIRED")
    void shouldTransitionValidLifecycle() {
        UUID appId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        JobApplication app = JobApplication.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.APPLIED)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        // 1. Review
        applicationService.updateApplicationStatus(appId, new UpdateApplicationStatusRequest(ApplicationStatus.UNDER_REVIEW, "Profile reviewed", null), adminId);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);

        // 2. Shortlist
        applicationService.updateApplicationStatus(appId, new UpdateApplicationStatusRequest(ApplicationStatus.SHORTLISTED, "Candidate shortlisted for technical round", null), adminId);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);

        // 3. Interview Scheduled
        applicationService.markInterviewScheduled(appId, adminId);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);

        // 4. Hire
        applicationService.updateApplicationStatus(appId, new UpdateApplicationStatusRequest(ApplicationStatus.HIRED, "Offer accepted", null), adminId);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.HIRED);
    }

    @Test
    @DisplayName("Should allow rejection from active states")
    void shouldAllowRejection() {
        UUID appId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        JobApplication app = JobApplication.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        applicationService.updateApplicationStatus(appId, new UpdateApplicationStatusRequest(ApplicationStatus.REJECTED, "Not matching required skills", "Insufficient experience"), adminId);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(app.getRejectionReason()).isEqualTo("Insufficient experience");
    }

    @Test
    @DisplayName("Should fail invalid transition from terminal HIRED state")
    void shouldFailTransitionFromTerminalState() {
        UUID appId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        JobApplication app = JobApplication.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.HIRED)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(appId, new UpdateApplicationStatusRequest(ApplicationStatus.UNDER_REVIEW, "test", null), adminId))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessageContaining("Cannot transition application from terminal status: HIRED");
    }

    @Test
    @DisplayName("Should verify interview eligibility only when SHORTLISTED")
    void shouldCheckInterviewEligibility() {
        UUID appId = UUID.randomUUID();
        JobApplication shortlisted = JobApplication.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(shortlisted));

        InterviewEligibilityResponse eligibility = applicationService.checkInterviewEligibility(appId);
        assertThat(eligibility.isEligible()).isTrue();

        shortlisted.setStatus(ApplicationStatus.APPLIED);
        InterviewEligibilityResponse notEligible = applicationService.checkInterviewEligibility(appId);
        assertThat(notEligible.isEligible()).isFalse();
    }

    @Test
    @DisplayName("Should return aggregated application stats")
    void shouldReturnStats() {
        when(applicationRepository.count()).thenReturn(10L);
        when(applicationRepository.countByStatus(ApplicationStatus.APPLIED)).thenReturn(4L);
        when(applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW)).thenReturn(2L);
        when(applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED)).thenReturn(2L);
        when(applicationRepository.countByStatus(ApplicationStatus.INTERVIEW_SCHEDULED)).thenReturn(1L);
        when(applicationRepository.countByStatus(ApplicationStatus.HIRED)).thenReturn(1L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN)).thenReturn(0L);

        ApplicationStatsResponse stats = applicationService.getStats();

        assertThat(stats.total()).isEqualTo(10);
        assertThat(stats.applied()).isEqualTo(4);
        assertThat(stats.shortlisted()).isEqualTo(2);
        assertThat(stats.hired()).isEqualTo(1);
    }
}
