package com.pi.job.repository;

import com.pi.job.entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, UUID> {

    List<JobSkill> findByJobId(UUID jobId);

    void deleteByJobId(UUID jobId);
}
