package com.pi.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateApplicationRequest(
    @NotNull(message = "Job ID is required")
    UUID jobId,

    @Size(max = 500, message = "Resume URL cannot exceed 500 characters")
    String resumeUrl,

    String coverNote
) {}
