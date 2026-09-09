package com.pi.application.dto;

import com.pi.application.enums.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
    UUID id,
    UUID applicationId,
    ApplicationStatus fromStatus,
    ApplicationStatus toStatus,
    UUID changedBy,
    String comments,
    Instant changedAt
) {}
