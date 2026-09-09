package com.pi.interview.service;

import com.pi.interview.client.ApplicationEligibility;
import com.pi.interview.client.ApplicationServiceClient;
import com.pi.interview.dto.CreateInterviewerRequest;
import com.pi.interview.dto.CreateSlotRequest;
import com.pi.interview.dto.InterviewHistoryResponse;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.InterviewerResponse;
import com.pi.interview.dto.ScheduleInterviewRequest;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.entity.Interview;
import com.pi.interview.entity.InterviewHistory;
import com.pi.interview.entity.InterviewSlot;
import com.pi.interview.entity.Interviewer;
import com.pi.interview.enums.ApplicationStatus;
import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.SlotStatus;
import com.pi.interview.exception.InterviewAccessDeniedException;
import com.pi.interview.exception.InterviewAlreadyExistsException;
import com.pi.interview.exception.InterviewNotAllowedException;
import com.pi.interview.exception.InterviewNotFoundException;
import com.pi.interview.exception.InterviewerInactiveException;
import com.pi.interview.exception.InterviewerNotFoundException;
import com.pi.interview.exception.InvalidInterviewStateException;
import com.pi.interview.exception.InvalidSlotException;
import com.pi.interview.exception.SlotAlreadyBookedException;
import com.pi.interview.exception.SlotExpiredException;
import com.pi.interview.exception.SlotNotFoundException;
import com.pi.interview.mapper.InterviewMapper;
import com.pi.interview.repository.InterviewHistoryRepository;
import com.pi.interview.repository.InterviewRepository;
import com.pi.interview.repository.InterviewSlotRepository;
import com.pi.interview.repository.InterviewerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewSlotRepository slotRepository;
    private final InterviewerRepository interviewerRepository;
    private final InterviewHistoryRepository historyRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final InterviewMapper interviewMapper;

    @Override
    public InterviewerResponse createInterviewer(CreateInterviewerRequest request) {
        Interviewer interviewer = Interviewer.builder()
                .userId(request.userId())
                .name(request.name())
                .email(request.email())
                .active(true)
                .build();
        Interviewer saved = interviewerRepository.save(interviewer);
        return interviewMapper.toInterviewerResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewerResponse> getActiveInterviewers() {
        return interviewerRepository.findByActiveTrue().stream()
                .map(interviewMapper::toInterviewerResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewerResponse getInterviewer(UUID interviewerId) {
        Interviewer interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new InterviewerNotFoundException("Interviewer not found with ID: " + interviewerId));
        return interviewMapper.toInterviewerResponse(interviewer);
    }

    @Override
    public SlotResponse createSlot(CreateSlotRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidSlotException("End time must be after start time");
        }

        Interviewer interviewer = interviewerRepository.findById(request.interviewerId())
                .orElseThrow(() -> new InterviewerNotFoundException("Interviewer not found with ID: " + request.interviewerId()));

        if (!interviewer.isActive()) {
            throw new InterviewerInactiveException("Interviewer is inactive");
        }

        InterviewSlot slot = InterviewSlot.builder()
                .interviewerId(request.interviewerId())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(SlotStatus.AVAILABLE)
                .build();

        InterviewSlot saved = slotRepository.save(slot);
        return interviewMapper.toSlotResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SlotResponse> getAvailableSlots(Pageable pageable) {
        return slotRepository.findAll(pageable)
                .map(interviewMapper::toSlotResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SlotResponse> getAvailableSlotsForApplication(UUID applicationId, Pageable pageable) {
        ApplicationEligibility eligibility =
                applicationServiceClient.getInterviewEligibility(applicationId);

        if (!eligibility.eligibleForInterview()) {
            throw new InterviewNotAllowedException("Candidate is not eligible for interview");
        }

        LocalDateTime now = LocalDateTime.now();
        List<SlotResponse> available = slotRepository.findByStatus(SlotStatus.AVAILABLE).stream()
                .filter(slot -> slot.getStartTime().isAfter(now))
                .map(interviewMapper::toSlotResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), available.size());
        List<SlotResponse> sublist = start > available.size() ? List.of() : available.subList(start, end);

        return new PageImpl<>(sublist, pageable, available.size());
    }

    @Override
    public InterviewResponse scheduleInterview(ScheduleInterviewRequest request, UUID candidateId) {
        // 1. Validate application eligibility
        ApplicationEligibility eligibility =
                applicationServiceClient.getInterviewEligibility(request.applicationId());

        // 2. Validate candidate ownership
        if (!eligibility.candidateId().equals(candidateId)) {
            throw new InterviewAccessDeniedException("Application does not belong to candidate");
        }

        // 3. Validate application status is SHORTLISTED
        if (!eligibility.eligibleForInterview() || eligibility.status() != ApplicationStatus.SHORTLISTED) {
            throw new InterviewNotAllowedException("Application is not eligible for interview (Status: " + eligibility.status() + ")");
        }

        // 4. Lock slot with pessimistic write lock
        InterviewSlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new SlotNotFoundException("Interview slot not found with ID: " + request.slotId()));

        // 5. Check slot availability
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotAlreadyBookedException("Interview slot is no longer available");
        }

        // 6. Check slot expiration
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            slot.setStatus(SlotStatus.EXPIRED);
            throw new SlotExpiredException("Interview slot has expired");
        }

        // 7. Check for existing active interview
        if (interviewRepository.existsByApplicationIdAndStatusNot(request.applicationId(), InterviewStatus.CANCELLED)) {
            throw new InterviewAlreadyExistsException("Active interview already exists for application");
        }

        // 8. Book the slot
        slot.setStatus(SlotStatus.BOOKED);

        // 9. Create interview record
        Interview interview = Interview.builder()
                .applicationId(request.applicationId())
                .candidateId(candidateId)
                .jobId(eligibility.jobId())
                .interviewerId(slot.getInterviewerId())
                .slotId(slot.getId())
                .roundNumber(request.roundNumber())
                .roundName(request.roundName())
                .interviewType(request.interviewType())
                .status(InterviewStatus.SCHEDULED)
                .scheduledStartTime(slot.getStartTime())
                .scheduledEndTime(slot.getEndTime())
                .meetingLink("https://meet.366pi.com/interview/" + UUID.randomUUID())
                .build();

        Interview saved = interviewRepository.save(interview);

        // 10. Record interview history
        InterviewHistory history = InterviewHistory.builder()
                .interviewId(saved.getId())
                .action("SCHEDULED")
                .newStartTime(saved.getScheduledStartTime())
                .newEndTime(saved.getScheduledEndTime())
                .newStatus(InterviewStatus.SCHEDULED.name())
                .performedBy(candidateId)
                .remarks("Interview scheduled by candidate")
                .build();
        historyRepository.save(history);

        // 11. Synchronize application status with Application Service
        applicationServiceClient.markInterviewScheduled(request.applicationId(), candidateId);

        log.info("Interview successfully scheduled with ID: {} for candidate: {}", saved.getId(), candidateId);
        return interviewMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterview(UUID interviewId, UUID candidateId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found with ID: " + interviewId));

        if (!interview.getCandidateId().equals(candidateId)) {
            throw new InterviewAccessDeniedException("You cannot access this interview");
        }

        return interviewMapper.toResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getMyInterviews(UUID candidateId, Pageable pageable) {
        return interviewRepository.findByCandidateId(candidateId, pageable)
                .map(interviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getAllInterviews(Pageable pageable) {
        return interviewRepository.findAll(pageable)
                .map(interviewMapper::toResponse);
    }

    @Override
    public InterviewResponse rescheduleInterview(UUID interviewId, UUID newSlotId, UUID adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found with ID: " + interviewId));

        if (interview.getStatus() == InterviewStatus.CANCELLED || interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new InvalidInterviewStateException("Interview cannot be rescheduled when " + interview.getStatus());
        }

        InterviewSlot newSlot = slotRepository.findByIdForUpdate(newSlotId)
                .orElseThrow(() -> new SlotNotFoundException("New slot not found with ID: " + newSlotId));

        if (newSlot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotAlreadyBookedException("New slot is not available");
        }

        if (!newSlot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new SlotExpiredException("New slot has already started or expired");
        }

        InterviewSlot oldSlot = slotRepository.findByIdForUpdate(interview.getSlotId())
                .orElseThrow(() -> new SlotNotFoundException("Old slot not found with ID: " + interview.getSlotId()));

        LocalDateTime oldStart = interview.getScheduledStartTime();
        LocalDateTime oldEnd = interview.getScheduledEndTime();

        // Release old slot and book new slot
        oldSlot.setStatus(SlotStatus.AVAILABLE);
        newSlot.setStatus(SlotStatus.BOOKED);

        interview.setSlotId(newSlot.getId());
        interview.setInterviewerId(newSlot.getInterviewerId());
        interview.setScheduledStartTime(newSlot.getStartTime());
        interview.setScheduledEndTime(newSlot.getEndTime());

        InterviewHistory history = InterviewHistory.builder()
                .interviewId(interview.getId())
                .action("RESCHEDULED")
                .oldStartTime(oldStart)
                .oldEndTime(oldEnd)
                .newStartTime(newSlot.getStartTime())
                .newEndTime(newSlot.getEndTime())
                .oldStatus(interview.getStatus().name())
                .newStatus(interview.getStatus().name())
                .performedBy(adminId)
                .remarks("Interview rescheduled by admin")
                .build();
        historyRepository.save(history);

        log.info("Interview {} rescheduled to new slot {}", interviewId, newSlotId);
        return interviewMapper.toResponse(interview);
    }

    @Override
    public InterviewResponse updateStatus(UUID interviewId, InterviewStatus newStatus, String notes, UUID actorId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found with ID: " + interviewId));

        InterviewStatus current = interview.getStatus();
        validateStatusTransition(current, newStatus);

        interview.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            interview.setNotes(notes);
        }

        InterviewHistory history = InterviewHistory.builder()
                .interviewId(interview.getId())
                .action("STATUS_CHANGED")
                .oldStatus(current.name())
                .newStatus(newStatus.name())
                .performedBy(actorId)
                .remarks(notes != null ? notes : "Status transitioned to " + newStatus)
                .build();
        historyRepository.save(history);

        log.info("Interview {} status transitioned from {} to {}", interviewId, current, newStatus);
        return interviewMapper.toResponse(interview);
    }

    @Override
    public InterviewResponse cancelInterview(UUID interviewId, UUID actorId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found with ID: " + interviewId));

        if (interview.getStatus() == InterviewStatus.CANCELLED || interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new InvalidInterviewStateException("Interview cannot be cancelled when already " + interview.getStatus());
        }

        InterviewStatus oldStatus = interview.getStatus();
        interview.setStatus(InterviewStatus.CANCELLED);

        // Release the slot
        slotRepository.findByIdForUpdate(interview.getSlotId()).ifPresent(slot -> {
            slot.setStatus(SlotStatus.AVAILABLE);
        });

        InterviewHistory history = InterviewHistory.builder()
                .interviewId(interview.getId())
                .action("CANCELLED")
                .oldStatus(oldStatus.name())
                .newStatus(InterviewStatus.CANCELLED.name())
                .performedBy(actorId)
                .remarks("Interview cancelled")
                .build();
        historyRepository.save(history);

        log.info("Interview {} cancelled by actor {}", interviewId, actorId);
        return interviewMapper.toResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewHistoryResponse> getInterviewHistory(UUID interviewId) {
        if (!interviewRepository.existsById(interviewId)) {
            throw new InterviewNotFoundException("Interview not found with ID: " + interviewId);
        }
        return historyRepository.findByInterviewIdOrderByCreatedAtAsc(interviewId).stream()
                .map(interviewMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(InterviewStatus current, InterviewStatus target) {
        if (current == target) {
            return;
        }
        boolean valid = switch (current) {
            case SCHEDULED -> target == InterviewStatus.CONFIRMED || target == InterviewStatus.CANCELLED;
            case CONFIRMED -> target == InterviewStatus.IN_PROGRESS || target == InterviewStatus.CANCELLED || target == InterviewStatus.NO_SHOW;
            case IN_PROGRESS -> target == InterviewStatus.COMPLETED || target == InterviewStatus.CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };

        if (!valid) {
            throw new InvalidInterviewStateException("Cannot transition interview status from " + current + " to " + target);
        }
    }
}
