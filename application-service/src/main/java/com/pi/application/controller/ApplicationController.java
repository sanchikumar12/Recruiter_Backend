package com.pi.application.controller;

import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.CreateApplicationRequest;
import com.pi.application.service.ApplicationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Candidate Application API", description = "Endpoints for candidates to apply for jobs and manage their submissions")
public class ApplicationController {

    private static final UUID DEFAULT_CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Apply for a job", description = "Submit a job application. Candidate ID is securely resolved from context.")
    public ApplicationResponse apply(
            @Valid @RequestBody CreateApplicationRequest request,
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID candidateId = resolveCandidateId(candidateHeader, userHeader, principal);
        return applicationService.applyForJob(request, candidateId);
    }

    @GetMapping("/my-applications")
    @Operation(summary = "Get my applications", description = "Retrieve all applications submitted by the current authenticated candidate")
    public Page<ApplicationResponse> getMyApplications(
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal,
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID candidateId = resolveCandidateId(candidateHeader, userHeader, principal);
        return applicationService.getCandidateApplications(candidateId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details", description = "Candidate retrieves their specific application by ID")
    public ApplicationResponse getApplication(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID candidateId = resolveCandidateId(candidateHeader, userHeader, principal);
        return applicationService.getApplicationById(id, candidateId, false);
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw application", description = "Withdraw application prior to shortlisting")
    public ApplicationResponse withdraw(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            Principal principal) {

        UUID candidateId = resolveCandidateId(candidateHeader, userHeader, principal);
        return applicationService.withdrawApplication(id, candidateId);
    }

    private UUID resolveCandidateId(String candidateHeader, String userHeader, Principal principal) {
        if (candidateHeader != null && !candidateHeader.isBlank()) {
            try { return UUID.fromString(candidateHeader.trim()); } catch (IllegalArgumentException ignored) {}
        }
        if (userHeader != null && !userHeader.isBlank()) {
            try { return UUID.fromString(userHeader.trim()); } catch (IllegalArgumentException ignored) {}
        }
        if (principal != null && principal.getName() != null) {
            try { return UUID.fromString(principal.getName().trim()); } catch (IllegalArgumentException ignored) {}
        }
        return DEFAULT_CANDIDATE_ID;
    }
}
