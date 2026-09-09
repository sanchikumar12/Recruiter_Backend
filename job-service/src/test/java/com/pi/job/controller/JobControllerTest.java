package com.pi.job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.job.dto.JobResponse;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import com.pi.job.exception.GlobalExceptionHandler;
import com.pi.job.exception.JobNotFoundException;
import com.pi.job.service.JobService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(GlobalExceptionHandler.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    @Test
    @DisplayName("GET /api/v1/jobs - Should return 200 with paged published jobs")
    void shouldReturnPublishedJobs() throws Exception {
        UUID id = UUID.randomUUID();
        JobResponse job = new JobResponse(
                id,
                "Senior Cloud Architect",
                "Design cloud scale microservices",
                "Platform",
                "Remote",
                WorkMode.REMOTE,
                EmploymentType.FULL_TIME,
                ExperienceLevel.SENIOR,
                new BigDecimal("5.0"),
                new BigDecimal("10.0"),
                new BigDecimal("2500000.00"),
                new BigDecimal("3500000.00"),
                "INR",
                JobStatus.PUBLISHED,
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                Instant.now(),
                List.of("AWS", "Kubernetes", "Java")
        );

        when(jobService.getPublishedJobs(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(job), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Senior Cloud Architect"))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId} - Should return 200 when published job exists")
    void shouldReturnJobWhenExists() throws Exception {
        UUID id = UUID.randomUUID();
        JobResponse job = new JobResponse(
                id,
                "Senior Cloud Architect",
                "Design cloud scale microservices",
                "Platform",
                "Remote",
                WorkMode.REMOTE,
                EmploymentType.FULL_TIME,
                ExperienceLevel.SENIOR,
                new BigDecimal("5.0"),
                new BigDecimal("10.0"),
                new BigDecimal("2500000.00"),
                new BigDecimal("3500000.00"),
                "INR",
                JobStatus.PUBLISHED,
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                Instant.now(),
                List.of("AWS", "Kubernetes", "Java")
        );

        when(jobService.getPublicJob(id)).thenReturn(job);

        mockMvc.perform(get("/api/v1/jobs/{jobId}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Senior Cloud Architect"));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId} - Should return 404 when job not found or not published")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.getPublicJob(id)).thenThrow(new JobNotFoundException(id));

        mockMvc.perform(get("/api/v1/jobs/{jobId}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
    }
}
