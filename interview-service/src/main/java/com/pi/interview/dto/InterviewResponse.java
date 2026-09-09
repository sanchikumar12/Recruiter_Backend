package com.pi.interview.dto;

import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.InterviewType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterviewResponse(
        UUID interviewId,
        UUID applicationId,
        UUID candidateId,
        UUID jobId,
        UUID interviewerId,
        UUID slotId,
        Integer roundNumber,
        String roundName,
        InterviewType interviewType,
        InterviewStatus status,
        String meetingLink,
        LocalDateTime scheduledStartTime,
        LocalDateTime scheduledEndTime,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
