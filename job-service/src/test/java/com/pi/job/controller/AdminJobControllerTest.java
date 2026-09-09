package com.pi.job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.job.dto.CreateJobRequest;
import com.pi.job.dto.JobResponse;
import com.pi.job.dto.JobStatsResponse;
import com.pi.job.dto.UpdateJobRequest;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import com.pi.job.exception.GlobalExceptionHandler;
import com.pi.job.exception.InvalidJobStateException;
import com.pi.job.exception.JobNotFoundException;
import com.pi.job.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminJobController.class)
@Import(GlobalExceptionHandler.class)
class AdminJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    @Test
    @DisplayName("POST /api/v1/admin/jobs - Should return 201 when request is valid")
    void shouldReturn201WhenCreateJobValid() throws Exception {
        UUID id = UUID.randomUUID();
        CreateJobRequest request = new CreateJobRequest(
                "Full Stack Engineer",
                "Full stack web development",
                "Product Engineering",
                "Pune",
                WorkMode.HYBRID,
                EmploymentType.FULL_TIME,
                ExperienceLevel.MID,
                new BigDecimal("2.0"),
                new BigDecimal("5.0"),
                new BigDecimal("800000.00"),
                new BigDecimal("1500000.00"),
                "INR",
                null,
                List.of("Java", "React", "Docker")
        );

        JobResponse response = new JobResponse(
                id,
                request.title(),
                request.description(),
                request.department(),
                request.location(),
                request.workMode(),
                request.employmentType(),
                request.experienceLevel(),
                request.minExperienceYears(),
                request.maxExperienceYears(),
                request.salaryMin(),
                request.salaryMax(),
                request.currency(),
                JobStatus.DRAFT,
                request.applicationDeadline(),
                UUID.randomUUID(),
                null,
                Instant.now(),
                Instant.now(),
                request.skills()
        );

        when(jobService.createJob(any(CreateJobRequest.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Full Stack Engineer"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/jobs - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "", // Blank title
                "", // Blank description
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of() // Empty skills
        );

        mockMvc.perform(post("/api/v1/admin/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/jobs/{jobId}/publish - Should return 204 No Content")
    void shouldReturn204WhenPublishing() throws Exception {
        UUID jobId = UUID.randomUUID();
        doNothing().when(jobService).publishJob(eq(jobId), any(UUID.class));

        mockMvc.perform(post("/api/v1/admin/jobs/{jobId}/publish", jobId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/admin/jobs/{jobId}/publish - Should return 400 on invalid state transition")
    void shouldReturn400WhenPublishFailsDueToInvalidState() throws Exception {
        UUID jobId = UUID.randomUUID();
        doThrow(new InvalidJobStateException("Job cannot be published from current state: CLOSED"))
                .when(jobService).publishJob(eq(jobId), any(UUID.class));

        mockMvc.perform(post("/api/v1/admin/jobs/{jobId}/publish", jobId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_STATE"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/jobs/{jobId}/pause - Should return 204 No Content")
    void shouldReturn204WhenPausing() throws Exception {
        UUID jobId = UUID.randomUUID();
        doNothing().when(jobService).pauseJob(eq(jobId), any(UUID.class));

        mockMvc.perform(post("/api/v1/admin/jobs/{jobId}/pause", jobId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/admin/jobs/{jobId}/close - Should return 204 No Content")
    void shouldReturn204WhenClosing() throws Exception {
        UUID jobId = UUID.randomUUID();
        doNothing().when(jobService).closeJob(eq(jobId), any(UUID.class));

        mockMvc.perform(post("/api/v1/admin/jobs/{jobId}/close", jobId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/admin/jobs/stats - Should return 200 with job status statistics")
    void shouldReturnJobStats() throws Exception {
        JobStatsResponse stats = new JobStatsResponse(10, 2, 5, 1, 2, 0);
        when(jobService.getJobStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/jobs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.draft").value(2))
                .andExpect(jsonPath("$.published").value(5))
                .andExpect(jsonPath("$.closed").value(2));
    }
}
