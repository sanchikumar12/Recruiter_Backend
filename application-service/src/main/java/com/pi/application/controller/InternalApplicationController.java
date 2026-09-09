package com.pi.application.controller;

import com.pi.application.dto.InterviewEligibilityResponse;
import com.pi.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/applications")
@RequiredArgsConstructor
@Tag(name = "Internal Application API", description = "Inter-service communication endpoints for Interview Service")
public class InternalApplicationController {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ApplicationService applicationService;

    @GetMapping("/{id}/eligibility")
    @Operation(summary = "Check interview eligibility", description = "Checks if candidate application is SHORTLISTED and eligible for interview scheduling")
    public InterviewEligibilityResponse checkEligibility(@PathVariable UUID id) {
        return applicationService.checkInterviewEligibility(id);
    }

    @PostMapping("/{id}/interview-scheduled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark interview scheduled", description = "Called by Interview Service once an interview slot is confirmed")
    public void markInterviewScheduled(
            @PathVariable UUID id,
            @RequestHeader(value = "X-System-Id", required = false) String systemHeader) {

        UUID changedBy = SYSTEM_USER_ID;
        if (systemHeader != null && !systemHeader.isBlank()) {
            try { changedBy = UUID.fromString(systemHeader.trim()); } catch (IllegalArgumentException ignored) {}
        }
        applicationService.markInterviewScheduled(id, changedBy);
    }
}
