package com.pi.interview.controller;

import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.ScheduleInterviewRequest;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.exception.InterviewAccessDeniedException;
import com.pi.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Tag(name = "Candidate Interview API", description = "Endpoints for candidates to view available slots and schedule interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/slots")
    @Operation(summary = "Get available slots for application", description = "Returns available future interview slots for an eligible shortlisted application")
    public Page<SlotResponse> getAvailableSlots(
            @RequestParam UUID applicationId,
            Pageable pageable) {
        return interviewService.getAvailableSlotsForApplication(applicationId, pageable);
    }

    @PostMapping
    @Operation(summary = "Schedule interview", description = "Candidate books an available slot to schedule their interview")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @Valid @RequestBody ScheduleInterviewRequest request,
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader) {

        UUID candidateId = resolveCandidateId(candidateHeader);
        InterviewResponse response = interviewService.scheduleInterview(request, candidateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my interviews", description = "Retrieve list of interviews scheduled for authenticated candidate")
    public Page<InterviewResponse> getMyInterviews(
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader,
            Pageable pageable) {

        UUID candidateId = resolveCandidateId(candidateHeader);
        return interviewService.getMyInterviews(candidateId, pageable);
    }

    @GetMapping("/{interviewId}")
    @Operation(summary = "Get interview details", description = "Candidate retrieves specific interview details")
    public InterviewResponse getInterview(
            @PathVariable UUID interviewId,
            @RequestHeader(value = "X-Candidate-Id", required = false) String candidateHeader) {

        UUID candidateId = resolveCandidateId(candidateHeader);
        return interviewService.getInterview(interviewId, candidateId);
    }

    private UUID resolveCandidateId(String candidateHeader) {
        if (candidateHeader != null && !candidateHeader.isBlank()) {
            try {
                return UUID.fromString(candidateHeader.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        throw new InterviewAccessDeniedException("Missing or invalid candidate credentials (X-Candidate-Id header required)");
    }
}
