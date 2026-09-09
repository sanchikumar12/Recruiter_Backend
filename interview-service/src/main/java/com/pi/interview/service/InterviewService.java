package com.pi.interview.service;

import com.pi.interview.dto.CreateInterviewerRequest;
import com.pi.interview.dto.CreateSlotRequest;
import com.pi.interview.dto.InterviewHistoryResponse;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.InterviewerResponse;
import com.pi.interview.dto.ScheduleInterviewRequest;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    InterviewerResponse createInterviewer(CreateInterviewerRequest request);

    List<InterviewerResponse> getActiveInterviewers();

    InterviewerResponse getInterviewer(UUID interviewerId);

    SlotResponse createSlot(CreateSlotRequest request);

    Page<SlotResponse> getAvailableSlots(Pageable pageable);

    Page<SlotResponse> getAvailableSlotsForApplication(UUID applicationId, Pageable pageable);

    InterviewResponse scheduleInterview(ScheduleInterviewRequest request, UUID candidateId);

    InterviewResponse getInterview(UUID interviewId, UUID candidateId);

    Page<InterviewResponse> getMyInterviews(UUID candidateId, Pageable pageable);

    Page<InterviewResponse> getAllInterviews(Pageable pageable);

    InterviewResponse rescheduleInterview(UUID interviewId, UUID newSlotId, UUID adminId);

    InterviewResponse updateStatus(UUID interviewId, InterviewStatus newStatus, String notes, UUID actorId);

    InterviewResponse cancelInterview(UUID interviewId, UUID actorId);

    List<InterviewHistoryResponse> getInterviewHistory(UUID interviewId);
}
