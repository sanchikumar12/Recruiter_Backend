package com.pi.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.CreateApplicationRequest;
import com.pi.application.enums.ApplicationStatus;
import com.pi.application.exception.GlobalExceptionHandler;
import com.pi.application.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(GlobalExceptionHandler.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationService applicationService;

    @Test
    @DisplayName("POST /api/v1/applications - Should return 201 when application valid")
    void shouldReturn201WhenApplyValid() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        CreateApplicationRequest request = new CreateApplicationRequest(jobId, "https://storage.366pi.com/resumes/c1.pdf", "Note");
        ApplicationResponse response = new ApplicationResponse(
                appId, jobId, candidateId, ApplicationStatus.APPLIED, request.resumeUrl(), request.coverNote(), null, Instant.now(), Instant.now()
        );

        when(applicationService.applyForJob(any(CreateApplicationRequest.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Candidate-Id", candidateId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(appId.toString()))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/my-applications - Should return 200 with candidate submissions")
    void shouldReturnMyApplications() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        ApplicationResponse response = new ApplicationResponse(
                appId, jobId, candidateId, ApplicationStatus.APPLIED, null, null, null, Instant.now(), Instant.now()
        );

        when(applicationService.getCandidateApplications(eq(candidateId), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/applications/my-applications")
                        .header("X-Candidate-Id", candidateId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(appId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("APPLIED"));
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/withdraw - Should return 200 with WITHDRAWN status")
    void shouldWithdrawApplication() throws Exception {
        UUID appId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        ApplicationResponse response = new ApplicationResponse(
                appId, UUID.randomUUID(), candidateId, ApplicationStatus.WITHDRAWN, null, null, null, Instant.now(), Instant.now()
        );

        when(applicationService.withdrawApplication(eq(appId), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/applications/{id}/withdraw", appId)
                        .header("X-Candidate-Id", candidateId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }
}
