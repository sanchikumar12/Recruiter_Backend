package com.pi.job.controller;

import com.pi.job.dto.CreateJobRequest;
import com.pi.job.dto.JobResponse;
import com.pi.job.dto.JobStatsResponse;
import com.pi.job.dto.UpdateJobRequest;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import com.pi.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
@Tag(name = "Admin Job API", description = "Endpoints for administrators and recruiters to create, update, and manage job openings")
public class AdminJobController {

    private static final UUID DEFAULT_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JobService jobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new job", description = "Creates a new job in DRAFT status. Admin identity is extracted from request context.")
    public JobResponse createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID adminId = resolveAdminId(adminHeader, userHeader, principal);
        return jobService.createJob(request, adminId);
    }

    @GetMapping
    @Operation(summary = "List all jobs (Admin)", description = "View and filter jobs across all statuses including DRAFT, PAUSED, and CLOSED")
    public Page<JobResponse> getAdminJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return jobService.getAdminJobs(
                keyword,
                location,
                workMode,
                employmentType,
                experienceLevel,
                minSalary,
                maxSalary,
                status,
                pageable
        );
    }

    @GetMapping("/stats")
    @Operation(summary = "Get job statistics", description = "Returns aggregated counts of jobs grouped by status")
    public JobStatsResponse getJobStats() {
        return jobService.getJobStats();
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get any job by ID", description = "View details of a job regardless of its status")
    public JobResponse getAdminJob(@PathVariable UUID jobId) {
        return jobService.getAdminJob(jobId);
    }

    @PutMapping("/{jobId}")
    @Operation(summary = "Update a job", description = "Updates job properties and skills")
    public JobResponse updateJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobRequest request,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID adminId = resolveAdminId(adminHeader, userHeader, principal);
        return jobService.updateJob(jobId, request, adminId);
    }

    @PostMapping("/{jobId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Publish a job", description = "Transitions a DRAFT or PAUSED job to PUBLISHED status")
    public void publishJob(
            @PathVariable UUID jobId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID adminId = resolveAdminId(adminHeader, userHeader, principal);
        jobService.publishJob(jobId, adminId);
    }

    @PostMapping("/{jobId}/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pause a job", description = "Temporarily hides a PUBLISHED job from candidates")
    public void pauseJob(
            @PathVariable UUID jobId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID adminId = resolveAdminId(adminHeader, userHeader, principal);
        jobService.pauseJob(jobId, adminId);
    }

    @PostMapping("/{jobId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close a job", description = "Permanently closes job recruitment")
    public void closeJob(
            @PathVariable UUID jobId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID adminId = resolveAdminId(adminHeader, userHeader, principal);
        jobService.closeJob(jobId, adminId);
    }

    private UUID resolveAdminId(String adminHeader, String userHeader, Principal principal) {
        if (adminHeader != null && !adminHeader.isBlank()) {
            try {
                return UUID.fromString(adminHeader.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        if (userHeader != null && !userHeader.isBlank()) {
            try {
                return UUID.fromString(userHeader.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        if (principal != null && principal.getName() != null) {
            try {
                return UUID.fromString(principal.getName().trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return DEFAULT_ADMIN_ID;
    }
}
