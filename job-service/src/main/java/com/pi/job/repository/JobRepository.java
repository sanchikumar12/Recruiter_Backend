package com.pi.job.repository;

import com.pi.job.entity.Job;
import com.pi.job.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository
        extends JpaRepository<Job, UUID>,
                JpaSpecificationExecutor<Job> {

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByCreatedBy(UUID createdBy, Pageable pageable);

    long countByStatus(JobStatus status);
}
