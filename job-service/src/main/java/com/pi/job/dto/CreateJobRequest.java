package com.pi.job.dto;

import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.WorkMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateJobRequest(
    @NotBlank
    @Size(max = 200)
    String title,

    @NotBlank
    String description,

    @Size(max = 100)
    String department,

    @Size(max = 200)
    String location,

    @NotNull
    WorkMode workMode,

    @NotNull
    EmploymentType employmentType,

    @NotNull
    ExperienceLevel experienceLevel,

    @DecimalMin("0.0")
    BigDecimal minExperienceYears,

    @DecimalMin("0.0")
    BigDecimal maxExperienceYears,

    @DecimalMin("0.0")
    BigDecimal salaryMin,

    @DecimalMin("0.0")
    BigDecimal salaryMax,

    String currency,

    Instant applicationDeadline,

    @NotEmpty
    List<String> skills
) {}
