package com.pi.interview.repository;

import com.pi.interview.entity.InterviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewHistoryRepository extends JpaRepository<InterviewHistory, UUID> {

    List<InterviewHistory> findByInterviewIdOrderByCreatedAtAsc(UUID interviewId);
}
