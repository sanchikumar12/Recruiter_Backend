package com.pi.job.dto;

import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
    UUID id,
    String title,
    String description,
    String department,
    String location,
    WorkMode workMode,
    EmploymentType employmentType,
    ExperienceLevel experienceLevel,
    BigDecimal minExperienceYears,
    BigDecimal maxExperienceYears,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String currency,
    JobStatus status,
    Instant applicationDeadline,
    UUID createdBy,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    List<String> skills
) {}
