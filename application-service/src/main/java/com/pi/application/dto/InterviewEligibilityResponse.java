package com.pi.application.dto;

import com.pi.application.enums.ApplicationStatus;

import java.util.UUID;

public record InterviewEligibilityResponse(
    UUID applicationId,
    UUID candidateId,
    UUID jobId,
    boolean isEligible,
    ApplicationStatus currentStatus,
    String message
) {}
