package com.pi.interview.dto;

import com.pi.interview.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewStatusRequest(
        @NotNull(message = "Interview status is required")
        InterviewStatus status,

        String notes
) {
}
