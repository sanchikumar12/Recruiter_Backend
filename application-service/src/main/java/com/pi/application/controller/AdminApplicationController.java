package com.pi.application.controller;

import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.ApplicationStatsResponse;
import com.pi.application.dto.ApplicationStatusHistoryResponse;
import com.pi.application.dto.UpdateApplicationStatusRequest;
import com.pi.application.enums.ApplicationStatus;
import com.pi.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/applications")
@RequiredArgsConstructor
@Tag(name = "Admin Application API", description = "Endpoints for recruiters and admins to review, shortlist, and manage job applications")
public class AdminApplicationController {

    private static final UUID DEFAULT_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ApplicationService applicationService;

    @GetMapping
    @Operation(summary = "List applications", description = "View applications with optional filtering by jobId and status")
    public Page<ApplicationResponse> getApplications(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return applicationService.getAdminApplications(jobId, status, pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get application metrics", description = "Returns aggregated application count grouped by lifecycle stage")
    public ApplicationStatsResponse getStats() {
        return applicationService.getStats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details", description = "Admin view of application details")
    public ApplicationResponse getApplication(@PathVariable UUID id) {
        return applicationService.getApplicationById(id, DEFAULT_ADMIN_ID, true);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status", description = "Transitions status (UNDER_REVIEW, SHORTLISTED, HIRED, REJECTED)")
    public ApplicationResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID adminId = resolveAdminId(adminHeader, userHeader, principal);
        return applicationService.updateApplicationStatus(id, request, adminId);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get application history", description = "Returns complete audit trail of status changes for an application")
    public List<ApplicationStatusHistoryResponse> getHistory(@PathVariable UUID id) {
        return applicationService.getApplicationHistory(id);
    }

    private UUID resolveAdminId(String adminHeader, String userHeader, Principal principal) {
        if (adminHeader != null && !adminHeader.isBlank()) {
            try { return UUID.fromString(adminHeader.trim()); } catch (IllegalArgumentException ignored) {}
        }
        if (userHeader != null && !userHeader.isBlank()) {
            try { return UUID.fromString(userHeader.trim()); } catch (IllegalArgumentException ignored) {}
        }
        if (principal != null && principal.getName() != null) {
            try { return UUID.fromString(principal.getName().trim()); } catch (IllegalArgumentException ignored) {}
        }
        return DEFAULT_ADMIN_ID;
    }
}
