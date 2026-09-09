package com.pi.interview.service;

import com.pi.interview.client.ApplicationEligibility;
import com.pi.interview.client.ApplicationServiceClient;
import com.pi.interview.dto.CreateSlotRequest;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.ScheduleInterviewRequest;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.entity.Interview;
import com.pi.interview.entity.InterviewSlot;
import com.pi.interview.entity.Interviewer;
import com.pi.interview.enums.ApplicationStatus;
import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.InterviewType;
import com.pi.interview.enums.SlotStatus;
import com.pi.interview.exception.InterviewAccessDeniedException;
import com.pi.interview.exception.InterviewNotAllowedException;
import com.pi.interview.exception.InterviewerInactiveException;
import com.pi.interview.exception.InvalidSlotException;
import com.pi.interview.exception.SlotAlreadyBookedException;
import com.pi.interview.mapper.InterviewMapper;
import com.pi.interview.repository.InterviewHistoryRepository;
import com.pi.interview.repository.InterviewRepository;
import com.pi.interview.repository.InterviewSlotRepository;
import com.pi.interview.repository.InterviewerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private InterviewSlotRepository slotRepository;

    @Mock
    private InterviewerRepository interviewerRepository;

    @Mock
    private InterviewHistoryRepository historyRepository;

    @Mock
    private ApplicationServiceClient applicationServiceClient;

    @Spy
    private InterviewMapper interviewMapper = new InterviewMapper();

    @InjectMocks
    private InterviewServiceImpl interviewService;

    private UUID interviewerId;
    private UUID candidateId;
    private UUID applicationId;
    private UUID jobId;
    private UUID slotId;

    @BeforeEach
    void setUp() {
        interviewerId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        slotId = UUID.randomUUID();
    }

    @Test
    void createSlot_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(45);
        CreateSlotRequest request = new CreateSlotRequest(interviewerId, start, end);

        Interviewer interviewer = Interviewer.builder()
                .id(interviewerId)
                .name("Interviewer Alice")
                .active(true)
                .build();

        InterviewSlot savedSlot = InterviewSlot.builder()
                .id(slotId)
                .interviewerId(interviewerId)
                .startTime(start)
                .endTime(end)
                .status(SlotStatus.AVAILABLE)
                .build();

        when(interviewerRepository.findById(interviewerId)).thenReturn(Optional.of(interviewer));
        when(slotRepository.save(any(InterviewSlot.class))).thenReturn(savedSlot);

        SlotResponse response = interviewService.createSlot(request);

        assertNotNull(response);
        assertEquals(slotId, response.slotId());
        assertEquals(SlotStatus.AVAILABLE, response.status());
        verify(slotRepository).save(any(InterviewSlot.class));
    }

    @Test
    void createSlot_EndTimeBeforeStartTime_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusMinutes(30);
        CreateSlotRequest request = new CreateSlotRequest(interviewerId, start, end);

        assertThrows(InvalidSlotException.class, () -> interviewService.createSlot(request));
    }

    @Test
    void createSlot_InactiveInterviewer_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(45);
        CreateSlotRequest request = new CreateSlotRequest(interviewerId, start, end);

        Interviewer interviewer = Interviewer.builder()
                .id(interviewerId)
                .name("Interviewer Bob")
                .active(false)
                .build();

        when(interviewerRepository.findById(interviewerId)).thenReturn(Optional.of(interviewer));

        assertThrows(InterviewerInactiveException.class, () -> interviewService.createSlot(request));
    }

    @Test
    void scheduleInterview_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusMinutes(45);

        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId, slotId, 1, "Technical Round 1", InterviewType.VIDEO
        );

        ApplicationEligibility eligibility = new ApplicationEligibility(
                applicationId, candidateId, jobId, ApplicationStatus.SHORTLISTED, true
        );

        InterviewSlot slot = InterviewSlot.builder()
                .id(slotId)
                .interviewerId(interviewerId)
                .startTime(start)
                .endTime(end)
                .status(SlotStatus.AVAILABLE)
                .build();

        UUID createdInterviewId = UUID.randomUUID();
        Interview savedInterview = Interview.builder()
                .id(createdInterviewId)
                .applicationId(applicationId)
                .candidateId(candidateId)
                .jobId(jobId)
                .interviewerId(interviewerId)
                .slotId(slotId)
                .roundNumber(1)
                .roundName("Technical Round 1")
                .interviewType(InterviewType.VIDEO)
                .status(InterviewStatus.SCHEDULED)
                .scheduledStartTime(start)
                .scheduledEndTime(end)
                .meetingLink("https://meet.366pi.com/interview/" + createdInterviewId)
                .build();

        when(applicationServiceClient.getInterviewEligibility(applicationId)).thenReturn(eligibility);
        when(slotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(interviewRepository.existsByApplicationIdAndStatusNot(applicationId, InterviewStatus.CANCELLED)).thenReturn(false);
        when(interviewRepository.save(any(Interview.class))).thenReturn(savedInterview);

        InterviewResponse response = interviewService.scheduleInterview(request, candidateId);

        assertNotNull(response);
        assertEquals(InterviewStatus.SCHEDULED, response.status());
        assertEquals(SlotStatus.BOOKED, slot.getStatus());
        verify(historyRepository).save(any());
        verify(applicationServiceClient).markInterviewScheduled(applicationId, candidateId);
    }

    @Test
    void scheduleInterview_CandidateMismatch_ThrowsAccessDenied() {
        UUID wrongCandidateId = UUID.randomUUID();
        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId, slotId, 1, "Tech Round", InterviewType.VIDEO
        );

        ApplicationEligibility eligibility = new ApplicationEligibility(
                applicationId, candidateId, jobId, ApplicationStatus.SHORTLISTED, true
        );

        when(applicationServiceClient.getInterviewEligibility(applicationId)).thenReturn(eligibility);

        assertThrows(InterviewAccessDeniedException.class,
                () -> interviewService.scheduleInterview(request, wrongCandidateId));
    }

    @Test
    void scheduleInterview_NotShortlisted_ThrowsException() {
        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId, slotId, 1, "Tech Round", InterviewType.VIDEO
        );

        ApplicationEligibility eligibility = new ApplicationEligibility(
                applicationId, candidateId, jobId, ApplicationStatus.APPLIED, false
        );

        when(applicationServiceClient.getInterviewEligibility(applicationId)).thenReturn(eligibility);

        assertThrows(InterviewNotAllowedException.class,
                () -> interviewService.scheduleInterview(request, candidateId));
    }

    @Test
    void scheduleInterview_AlreadyBookedSlot_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusMinutes(45);

        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId, slotId, 1, "Tech Round", InterviewType.VIDEO
        );

        ApplicationEligibility eligibility = new ApplicationEligibility(
                applicationId, candidateId, jobId, ApplicationStatus.SHORTLISTED, true
        );

        InterviewSlot slot = InterviewSlot.builder()
                .id(slotId)
                .interviewerId(interviewerId)
                .startTime(start)
                .endTime(end)
                .status(SlotStatus.BOOKED)
                .build();

        when(applicationServiceClient.getInterviewEligibility(applicationId)).thenReturn(eligibility);
        when(slotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));

        assertThrows(SlotAlreadyBookedException.class,
                () -> interviewService.scheduleInterview(request, candidateId));
    }

    @Test
    void cancelInterview_Success() {
        UUID interviewId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Interview interview = Interview.builder()
                .id(interviewId)
                .applicationId(applicationId)
                .candidateId(candidateId)
                .slotId(slotId)
                .status(InterviewStatus.SCHEDULED)
                .build();

        InterviewSlot slot = InterviewSlot.builder()
                .id(slotId)
                .status(SlotStatus.BOOKED)
                .build();

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(slotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));

        InterviewResponse response = interviewService.cancelInterview(interviewId, adminId);

        assertNotNull(response);
        assertEquals(InterviewStatus.CANCELLED, interview.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
        verify(historyRepository).save(any());
    }
}
