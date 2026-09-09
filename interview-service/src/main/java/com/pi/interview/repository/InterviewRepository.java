package com.pi.interview.repository;

import com.pi.interview.entity.Interview;
import com.pi.interview.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    Page<Interview> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<Interview> findByInterviewerId(UUID interviewerId, Pageable pageable);

    Page<Interview> findByJobId(UUID jobId, Pageable pageable);

    Page<Interview> findByStatus(InterviewStatus status, Pageable pageable);

    boolean existsByApplicationIdAndStatusNot(UUID applicationId, InterviewStatus status);
}
