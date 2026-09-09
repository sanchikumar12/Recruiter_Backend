package com.pi.interview.controller;

import com.pi.interview.dto.CreateInterviewerRequest;
import com.pi.interview.dto.InterviewerResponse;
import com.pi.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/interviewers")
@RequiredArgsConstructor
@Tag(name = "Admin Interviewer API", description = "Endpoints for managing active interviewers")
public class InterviewerController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register interviewer", description = "Admin registers a user as an active interviewer")
    public InterviewerResponse createInterviewer(@Valid @RequestBody CreateInterviewerRequest request) {
        return interviewService.createInterviewer(request);
    }

    @GetMapping
    @Operation(summary = "List active interviewers", description = "Retrieve list of all active interviewers")
    public List<InterviewerResponse> getActiveInterviewers() {
        return interviewService.getActiveInterviewers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get interviewer details", description = "Retrieve interviewer by ID")
    public InterviewerResponse getInterviewer(@PathVariable UUID id) {
        return interviewService.getInterviewer(id);
    }
}
