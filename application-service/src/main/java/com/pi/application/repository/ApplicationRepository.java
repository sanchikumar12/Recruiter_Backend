package com.pi.application.repository;

import com.pi.application.entity.JobApplication;
import com.pi.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<JobApplication, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    Optional<JobApplication> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    Page<JobApplication> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<JobApplication> findByJobId(UUID jobId, Pageable pageable);

    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByJobIdAndStatus(UUID jobId, ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);
}
