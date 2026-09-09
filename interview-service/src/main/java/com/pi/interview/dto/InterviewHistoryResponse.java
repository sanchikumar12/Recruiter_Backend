package com.pi.interview.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterviewHistoryResponse(
        UUID id,
        UUID interviewId,
        String action,
        LocalDateTime oldStartTime,
        LocalDateTime oldEndTime,
        LocalDateTime newStartTime,
        LocalDateTime newEndTime,
        String oldStatus,
        String newStatus,
        UUID performedBy,
        String remarks,
        LocalDateTime createdAt
) {
}
