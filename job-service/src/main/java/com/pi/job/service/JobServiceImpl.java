package com.pi.job.service;

import com.pi.job.dto.CreateJobRequest;
import com.pi.job.dto.JobResponse;
import com.pi.job.dto.JobStatsResponse;
import com.pi.job.dto.UpdateJobRequest;
import com.pi.job.entity.Job;
import com.pi.job.entity.JobSkill;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import com.pi.job.exception.InvalidJobStateException;
import com.pi.job.exception.JobNotFoundException;
import com.pi.job.mapper.JobMapper;
import com.pi.job.repository.JobRepository;
import com.pi.job.repository.JobSkillRepository;
import com.pi.job.specification.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobMapper jobMapper;

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request, UUID adminId) {
        validateSalary(request.salaryMin(), request.salaryMax());
        validateExperience(request.minExperienceYears(), request.maxExperienceYears());

        Instant now = Instant.now();
        UUID jobId = UUID.randomUUID();

        Job job = Job.builder()
                .id(jobId)
                .title(request.title().trim())
                .description(request.description())
                .department(request.department() != null ? request.department().trim() : null)
                .location(request.location() != null ? request.location().trim() : null)
                .workMode(request.workMode())
                .employmentType(request.employmentType())
                .experienceLevel(request.experienceLevel())
                .minExperienceYears(request.minExperienceYears())
                .maxExperienceYears(request.maxExperienceYears())
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .currency(request.currency() != null ? request.currency().trim() : "INR")
                .status(JobStatus.DRAFT)
                .applicationDeadline(request.applicationDeadline())
                .createdBy(adminId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        jobRepository.save(job);

        List<String> skillStrings = List.of();
        if (request.skills() != null && !request.skills().isEmpty()) {
            List<JobSkill> skills = request.skills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .map(s -> JobSkill.builder()
                            .id(UUID.randomUUID())
                            .jobId(jobId)
                            .skill(s)
                            .required(true)
                            .build())
                    .toList();
            jobSkillRepository.saveAll(skills);
            skillStrings = skills.stream().map(JobSkill::getSkill).toList();
        }

        log.info("Job created with ID: {} by admin: {}", jobId, adminId);
        return jobMapper.toResponse(job, skillStrings);
    }

    @Override
    public JobResponse getPublicJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        // Public users can only see PUBLISHED jobs
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new JobNotFoundException(jobId);
        }

        List<String> skills = jobSkillRepository.findByJobId(jobId).stream()
                .map(JobSkill::getSkill)
                .toList();

        return jobMapper.toResponse(job, skills);
    }

    @Override
    public JobResponse getAdminJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        List<String> skills = jobSkillRepository.findByJobId(jobId).stream()
                .map(JobSkill::getSkill)
                .toList();

        return jobMapper.toResponse(job, skills);
    }

    @Override
    public Page<JobResponse> getPublishedJobs(
            String keyword,
            String location,
            WorkMode workMode,
            EmploymentType employmentType,
            ExperienceLevel experienceLevel,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            Pageable pageable) {

        Specification<Job> spec = JobSpecification.filterJobs(
                keyword,
                location,
                workMode,
                employmentType,
                experienceLevel,
                minSalary,
                maxSalary,
                JobStatus.PUBLISHED
        );

        return jobRepository.findAll(spec, pageable)
                .map(job -> {
                    List<String> skills = jobSkillRepository.findByJobId(job.getId()).stream()
                            .map(JobSkill::getSkill)
                            .toList();
                    return jobMapper.toResponse(job, skills);
                });
    }

    @Override
    public Page<JobResponse> getAdminJobs(
            String keyword,
            String location,
            WorkMode workMode,
            EmploymentType employmentType,
            ExperienceLevel experienceLevel,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            JobStatus status,
            Pageable pageable) {

        Specification<Job> spec = JobSpecification.filterJobs(
                keyword,
                location,
                workMode,
                employmentType,
                experienceLevel,
                minSalary,
                maxSalary,
                status
        );

        return jobRepository.findAll(spec, pageable)
                .map(job -> {
                    List<String> skills = jobSkillRepository.findByJobId(job.getId()).stream()
                            .map(JobSkill::getSkill)
                            .toList();
                    return jobMapper.toResponse(job, skills);
                });
    }

    @Override
    @Transactional
    public JobResponse updateJob(UUID jobId, UpdateJobRequest request, UUID adminId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new InvalidJobStateException("Cannot modify a closed job");
        }

        validateSalary(request.salaryMin(), request.salaryMax());
        validateExperience(request.minExperienceYears(), request.maxExperienceYears());

        job.setTitle(request.title().trim());
        job.setDescription(request.description());
        job.setDepartment(request.department() != null ? request.department().trim() : null);
        job.setLocation(request.location() != null ? request.location().trim() : null);
        job.setWorkMode(request.workMode());
        job.setEmploymentType(request.employmentType());
        job.setExperienceLevel(request.experienceLevel());
        job.setMinExperienceYears(request.minExperienceYears());
        job.setMaxExperienceYears(request.maxExperienceYears());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        if (request.currency() != null) {
            job.setCurrency(request.currency().trim());
        }
        job.setApplicationDeadline(request.applicationDeadline());
        job.setUpdatedAt(Instant.now());

        jobRepository.save(job);

        List<String> skillStrings = List.of();
        if (request.skills() != null) {
            jobSkillRepository.deleteByJobId(jobId);
            List<JobSkill> skills = request.skills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .map(s -> JobSkill.builder()
                            .id(UUID.randomUUID())
                            .jobId(jobId)
                            .skill(s)
                            .required(true)
                            .build())
                    .toList();
            jobSkillRepository.saveAll(skills);
            skillStrings = skills.stream().map(JobSkill::getSkill).toList();
        } else {
            skillStrings = jobSkillRepository.findByJobId(jobId).stream()
                    .map(JobSkill::getSkill)
                    .toList();
        }

        log.info("Job updated: {} by admin: {}", jobId, adminId);
        return jobMapper.toResponse(job, skillStrings);
    }

    @Override
    @Transactional
    public void publishJob(UUID jobId, UUID adminId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.PAUSED) {
            throw new InvalidJobStateException("Job cannot be published from current state: " + job.getStatus());
        }

        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(Instant.now())) {
            throw new InvalidJobStateException("Application deadline has already passed");
        }

        Instant now = Instant.now();
        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(now);
        job.setUpdatedAt(now);
        jobRepository.save(job);

        log.info("Job published: {} by admin: {}", jobId, adminId);
    }

    @Override
    @Transactional
    public void pauseJob(UUID jobId, UUID adminId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new InvalidJobStateException("Job cannot be paused from current state: " + job.getStatus());
        }

        job.setStatus(JobStatus.PAUSED);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job paused: {} by admin: {}", jobId, adminId);
    }

    @Override
    @Transactional
    public void closeJob(UUID jobId, UUID adminId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new InvalidJobStateException("Job is already closed");
        }

        job.setStatus(JobStatus.CLOSED);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job closed: {} by admin: {}", jobId, adminId);
    }

    @Override
    public JobStatsResponse getJobStats() {
        long total = jobRepository.count();
        long draft = jobRepository.countByStatus(JobStatus.DRAFT);
        long published = jobRepository.countByStatus(JobStatus.PUBLISHED);
        long paused = jobRepository.countByStatus(JobStatus.PAUSED);
        long closed = jobRepository.countByStatus(JobStatus.CLOSED);
        long expired = jobRepository.countByStatus(JobStatus.EXPIRED);

        return new JobStatsResponse(total, draft, published, paused, closed, expired);
    }

    private void validateSalary(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
        }
    }

    private void validateExperience(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimum experience cannot exceed maximum experience");
        }
    }
}
