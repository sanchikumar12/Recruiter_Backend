package com.pi.interview.controller;

import com.pi.interview.dto.CreateSlotRequest;
import com.pi.interview.dto.InterviewHistoryResponse;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.RescheduleInterviewRequest;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.dto.UpdateInterviewStatusRequest;
import com.pi.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/interviews")
@RequiredArgsConstructor
@Tag(name = "Admin Interview API", description = "Endpoints for recruiters/admins to manage interview slots, schedules, statuses, and audit history")
public class AdminInterviewController {

    private static final UUID DEFAULT_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final InterviewService interviewService;

    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create interview slot", description = "Admin defines an interview slot for an active interviewer")
    public SlotResponse createSlot(@Valid @RequestBody CreateSlotRequest request) {
        return interviewService.createSlot(request);
    }

    @GetMapping("/slots")
    @Operation(summary = "Get all interview slots", description = "Admin queries paged list of interview slots across all statuses")
    public Page<SlotResponse> getAllSlots(Pageable pageable) {
        return interviewService.getAvailableSlots(pageable);
    }

    @GetMapping
    @Operation(summary = "Get all interviews", description = "Admin queries paged list of all interviews")
    public Page<InterviewResponse> getAllInterviews(Pageable pageable) {
        return interviewService.getAllInterviews(pageable);
    }

    @PatchMapping("/{interviewId}/status")
    @Operation(summary = "Update interview status", description = "Admin transitions interview status (CONFIRMED, IN_PROGRESS, COMPLETED, NO_SHOW)")
    public InterviewResponse updateStatus(
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewStatusRequest request,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader) {

        UUID adminId = resolveAdminId(adminHeader);
        return interviewService.updateStatus(interviewId, request.status(), request.notes(), adminId);
    }

    @PostMapping("/{interviewId}/reschedule")
    @Operation(summary = "Reschedule interview", description = "Admin moves an interview to a new available slot")
    public InterviewResponse reschedule(
            @PathVariable UUID interviewId,
            @Valid @RequestBody RescheduleInterviewRequest request,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader) {

        UUID adminId = resolveAdminId(adminHeader);
        return interviewService.rescheduleInterview(interviewId, request.newSlotId(), adminId);
    }

    @PostMapping("/{interviewId}/cancel")
    @Operation(summary = "Cancel interview", description = "Admin cancels an interview and releases the associated slot")
    public InterviewResponse cancel(
            @PathVariable UUID interviewId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminHeader) {

        UUID adminId = resolveAdminId(adminHeader);
        return interviewService.cancelInterview(interviewId, adminId);
    }

    @GetMapping("/{interviewId}/history")
    @Operation(summary = "Get interview history", description = "Retrieve full chronological audit history of interview state changes")
    public List<InterviewHistoryResponse> getHistory(@PathVariable UUID interviewId) {
        return interviewService.getInterviewHistory(interviewId);
    }

    private UUID resolveAdminId(String adminHeader) {
        if (adminHeader != null && !adminHeader.isBlank()) {
            try {
                return UUID.fromString(adminHeader.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return DEFAULT_ADMIN_ID;
    }
}
