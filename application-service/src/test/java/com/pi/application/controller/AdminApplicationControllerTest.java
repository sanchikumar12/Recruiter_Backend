package com.pi.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.ApplicationStatsResponse;
import com.pi.application.dto.ApplicationStatusHistoryResponse;
import com.pi.application.dto.UpdateApplicationStatusRequest;
import com.pi.application.enums.ApplicationStatus;
import com.pi.application.exception.GlobalExceptionHandler;
import com.pi.application.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminApplicationController.class)
@Import(GlobalExceptionHandler.class)
class AdminApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationService applicationService;

    @Test
    @DisplayName("PUT /api/v1/admin/applications/{id}/status - Should return 200 with updated status")
    void shouldUpdateStatusSuccessfully() throws Exception {
        UUID appId = UUID.randomUUID();
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest(ApplicationStatus.SHORTLISTED, "Candidate has strong Java skills", null);

        ApplicationResponse response = new ApplicationResponse(
                appId, UUID.randomUUID(), UUID.randomUUID(), ApplicationStatus.SHORTLISTED, null, null, null, Instant.now(), Instant.now()
        );

        when(applicationService.updateApplicationStatus(eq(appId), any(UpdateApplicationStatusRequest.class), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/applications/{id}/status", appId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/applications/{id}/history - Should return 200 with audit entries")
    void shouldReturnHistory() throws Exception {
        UUID appId = UUID.randomUUID();
        ApplicationStatusHistoryResponse entry = new ApplicationStatusHistoryResponse(
                UUID.randomUUID(), appId, ApplicationStatus.APPLIED, ApplicationStatus.UNDER_REVIEW, UUID.randomUUID(), "Reviewing profile", Instant.now()
        );

        when(applicationService.getApplicationHistory(appId)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/admin/applications/{id}/history", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromStatus").value("APPLIED"))
                .andExpect(jsonPath("$[0].toStatus").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/applications/stats - Should return 200 with metrics")
    void shouldReturnStats() throws Exception {
        ApplicationStatsResponse stats = new ApplicationStatsResponse(15, 5, 4, 3, 1, 1, 1, 0);
        when(applicationService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/applications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.applied").value(5))
                .andExpect(jsonPath("$.shortlisted").value(3));
    }
}
