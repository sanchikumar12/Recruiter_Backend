package com.pi.interview.dto;

import com.pi.interview.enums.InterviewType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ScheduleInterviewRequest(
        @NotNull(message = "Application ID is required")
        UUID applicationId,

        @NotNull(message = "Slot ID is required")
        UUID slotId,

        @NotNull(message = "Round number is required")
        Integer roundNumber,

        @NotNull(message = "Round name is required")
        @Size(max = 255, message = "Round name must not exceed 255 characters")
        String roundName,

        @NotNull(message = "Interview type is required")
        InterviewType interviewType
) {
}
