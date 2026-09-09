package com.pi.application.dto;

import com.pi.application.enums.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID jobId,
    UUID candidateId,
    ApplicationStatus status,
    String resumeUrl,
    String coverNote,
    String rejectionReason,
    Instant appliedAt,
    Instant updatedAt
) {}
