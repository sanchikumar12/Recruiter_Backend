package com.pi.job.service;

import com.pi.job.dto.CreateJobRequest;
import com.pi.job.dto.JobResponse;
import com.pi.job.dto.JobStatsResponse;
import com.pi.job.dto.UpdateJobRequest;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface JobService {

    JobResponse createJob(CreateJobRequest request, UUID adminId);

    JobResponse getPublicJob(UUID jobId);

    JobResponse getAdminJob(UUID jobId);

    Page<JobResponse> getPublishedJobs(
            String keyword,
            String location,
            WorkMode workMode,
            EmploymentType employmentType,
            ExperienceLevel experienceLevel,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            Pageable pageable
    );

    Page<JobResponse> getAdminJobs(
            String keyword,
            String location,
            WorkMode workMode,
            EmploymentType employmentType,
            ExperienceLevel experienceLevel,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            JobStatus status,
            Pageable pageable
    );

    JobResponse updateJob(UUID jobId, UpdateJobRequest request, UUID adminId);

    void publishJob(UUID jobId, UUID adminId);

    void pauseJob(UUID jobId, UUID adminId);

    void closeJob(UUID jobId, UUID adminId);

    JobStatsResponse getJobStats();
}
