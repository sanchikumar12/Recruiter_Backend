package com.pi.interview.repository;

import com.pi.interview.entity.InterviewSlot;
import com.pi.interview.enums.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InterviewSlot s WHERE s.id = :slotId")
    Optional<InterviewSlot> findByIdForUpdate(@Param("slotId") UUID slotId);

    List<InterviewSlot> findByStatus(SlotStatus status);

    List<InterviewSlot> findByInterviewerIdAndStatus(UUID interviewerId, SlotStatus status);
}
