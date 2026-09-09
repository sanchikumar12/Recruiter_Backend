package com.pi.job.controller;

import com.pi.job.dto.JobResponse;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.WorkMode;
import com.pi.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Candidate Job API", description = "Public endpoints for candidates to browse and search published jobs")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "Get published jobs", description = "List or search published job openings with filtering and pagination")
    public Page<JobResponse> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return jobService.getPublishedJobs(
                keyword,
                location,
                workMode,
                employmentType,
                experienceLevel,
                minSalary,
                maxSalary,
                pageable
        );
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job details", description = "Retrieve detailed information for a specific published job opening")
    public JobResponse getJob(@PathVariable UUID jobId) {
        return jobService.getPublicJob(jobId);
    }
}
