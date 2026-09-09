package com.pi.job.service;

import com.pi.job.dto.CreateJobRequest;
import com.pi.job.dto.JobResponse;
import com.pi.job.dto.JobStatsResponse;
import com.pi.job.dto.UpdateJobRequest;
import com.pi.job.entity.Job;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import com.pi.job.exception.InvalidJobStateException;
import com.pi.job.exception.JobNotFoundException;
import com.pi.job.mapper.JobMapper;
import com.pi.job.repository.JobRepository;
import com.pi.job.repository.JobSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    private JobMapper jobMapper;
    private JobServiceImpl jobService;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        jobMapper = new JobMapper();
        jobService = new JobServiceImpl(jobRepository, jobSkillRepository, jobMapper);
        adminId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully create a job in DRAFT status")
    void shouldCreateJobSuccessfully() {
        CreateJobRequest request = new CreateJobRequest(
                "Java Backend Engineer",
                "Building high throughput services",
                "Engineering",
                "Bangalore",
                WorkMode.HYBRID,
                EmploymentType.FULL_TIME,
                ExperienceLevel.ENTRY,
                new BigDecimal("1.0"),
                new BigDecimal("3.0"),
                new BigDecimal("600000.00"),
                new BigDecimal("1000000.00"),
                "INR",
                Instant.now().plus(30, ChronoUnit.DAYS),
                List.of("Java", "Spring Boot", "PostgreSQL")
        );

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.createJob(request, adminId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Java Backend Engineer");
        assertThat(response.status()).isEqualTo(JobStatus.DRAFT);
        assertThat(response.createdBy()).isEqualTo(adminId);
        assertThat(response.skills()).containsExactlyInAnyOrder("Java", "Spring Boot", "PostgreSQL");

        verify(jobRepository).save(any(Job.class));
        verify(jobSkillRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when minimum salary exceeds maximum salary")
    void shouldFailWhenSalaryMinExceedsSalaryMax() {
        CreateJobRequest request = new CreateJobRequest(
                "Java Backend Engineer",
                "Building high throughput services",
                "Engineering",
                "Bangalore",
                WorkMode.HYBRID,
                EmploymentType.FULL_TIME,
                ExperienceLevel.ENTRY,
                new BigDecimal("1.0"),
                new BigDecimal("3.0"),
                new BigDecimal("1200000.00"),
                new BigDecimal("800000.00"),
                "INR",
                null,
                List.of("Java")
        );

        assertThatThrownBy(() -> jobService.createJob(request, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum salary cannot exceed maximum salary");
    }

    @Test
    @DisplayName("Should throw exception when minimum experience exceeds maximum experience")
    void shouldFailWhenMinExpExceedsMaxExp() {
        CreateJobRequest request = new CreateJobRequest(
                "Java Backend Engineer",
                "Building high throughput services",
                "Engineering",
                "Bangalore",
                WorkMode.HYBRID,
                EmploymentType.FULL_TIME,
                ExperienceLevel.ENTRY,
                new BigDecimal("5.0"),
                new BigDecimal("2.0"),
                null,
                null,
                "INR",
                null,
                List.of("Java")
        );

        assertThatThrownBy(() -> jobService.createJob(request, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum experience cannot exceed maximum experience");
    }

    @Test
    @DisplayName("Should successfully publish a DRAFT job")
    void shouldPublishJobSuccessfully() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.DRAFT)
                .createdBy(adminId)
                .applicationDeadline(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        jobService.publishJob(jobId, adminId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(job.getPublishedAt()).isNotNull();
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Should throw exception when publishing a job with past deadline")
    void shouldFailPublishingWhenDeadlinePassed() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.DRAFT)
                .createdBy(adminId)
                .applicationDeadline(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.publishJob(jobId, adminId))
                .isInstanceOf(InvalidJobStateException.class)
                .hasMessageContaining("Application deadline has already passed");
    }

    @Test
    @DisplayName("Should successfully pause a PUBLISHED job")
    void shouldPauseJobSuccessfully() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.PUBLISHED)
                .createdBy(adminId)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        jobService.pauseJob(jobId, adminId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.PAUSED);
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Should fail pausing a job that is in DRAFT status")
    void shouldFailPausingDraftJob() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.DRAFT)
                .createdBy(adminId)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.pauseJob(jobId, adminId))
                .isInstanceOf(InvalidJobStateException.class)
                .hasMessageContaining("Job cannot be paused from current state: DRAFT");
    }

    @Test
    @DisplayName("Should successfully close a PUBLISHED job")
    void shouldCloseJobSuccessfully() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.PUBLISHED)
                .createdBy(adminId)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        jobService.closeJob(jobId, adminId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Should fail closing an already CLOSED job")
    void shouldFailClosingAlreadyClosedJob() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.CLOSED)
                .createdBy(adminId)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.closeJob(jobId, adminId))
                .isInstanceOf(InvalidJobStateException.class)
                .hasMessageContaining("Job is already closed");
    }

    @Test
    @DisplayName("Public getJob should throw not found for DRAFT jobs")
    void shouldNotAllowPublicAccessToDraftJob() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .status(JobStatus.DRAFT)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.getPublicJob(jobId))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    @DisplayName("Should retrieve aggregated job statistics")
    void shouldReturnJobStats() {
        when(jobRepository.count()).thenReturn(25L);
        when(jobRepository.countByStatus(JobStatus.DRAFT)).thenReturn(5L);
        when(jobRepository.countByStatus(JobStatus.PUBLISHED)).thenReturn(12L);
        when(jobRepository.countByStatus(JobStatus.PAUSED)).thenReturn(2L);
        when(jobRepository.countByStatus(JobStatus.CLOSED)).thenReturn(4L);
        when(jobRepository.countByStatus(JobStatus.EXPIRED)).thenReturn(2L);

        JobStatsResponse stats = jobService.getJobStats();

        assertThat(stats.total()).isEqualTo(25);
        assertThat(stats.draft()).isEqualTo(5);
        assertThat(stats.published()).isEqualTo(12);
        assertThat(stats.paused()).isEqualTo(2);
        assertThat(stats.closed()).isEqualTo(4);
        assertThat(stats.expired()).isEqualTo(2);
    }
}
