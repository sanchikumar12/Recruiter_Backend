package com.pi.job.mapper;

import com.pi.job.dto.JobResponse;
import com.pi.job.entity.Job;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobMapper {

    public JobResponse toResponse(Job job, List<String> skills) {
        if (job == null) {
            return null;
        }

        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getDepartment(),
                job.getLocation(),
                job.getWorkMode(),
                job.getEmploymentType(),
                job.getExperienceLevel(),
                job.getMinExperienceYears(),
                job.getMaxExperienceYears(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getStatus(),
                job.getApplicationDeadline(),
                job.getCreatedBy(),
                job.getPublishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                skills != null ? skills : List.of()
        );
    }
}
