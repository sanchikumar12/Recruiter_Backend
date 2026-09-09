package com.pi.application.dto;

import com.pi.application.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
    @NotNull(message = "Status is required")
    ApplicationStatus status,

    String comments,

    String rejectionReason
) {}
