package com.pi.interview.repository;

import com.pi.interview.entity.Interviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewerRepository extends JpaRepository<Interviewer, UUID> {

    List<Interviewer> findByActiveTrue();

    Optional<Interviewer> findByIdAndActiveTrue(UUID id);
}
