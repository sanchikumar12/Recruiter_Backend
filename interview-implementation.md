# Interview Service - Implementation Guide

## 1. Overview

The Interview Service is responsible for managing the interview scheduling lifecycle of the 366PI recruitment platform.

It handles:

- Interviewer availability
- Interview slots
- Candidate slot selection
- Interview scheduling
- Interview rescheduling
- Interview cancellation
- Interview status
- Interview meeting links
- Interview rounds
- Interview history
- Preventing double booking
- Communicating with Application Service
- Future notifications through Kafka

The most important rule is:

> Application Service owns the application. Interview Service owns the interview.

The Interview Service must NOT directly access `application_db`.

---

# 2. Position in 366PI Architecture

The recruitment flow is:

    Candidate
        |
        v
    Frontend
        |
        v
    API Gateway
        |
        +--------------------+
        |                    |
        v                    v
    Job Service       Application Service
                           |
                           |
                    Candidate shortlisted
                           |
                           v
                    Interview Service
                           |
                           v
                     interview_db
                           |
                           v
                  Notification Service


Infrastructure:

    Eureka Server
         |
         +-- Auth Service
         +-- User Service
         +-- Job Service
         +-- Application Service
         +-- Interview Service
         +-- Notification Service

    Spring Boot Admin
         |
         +-- Interview Service

    Kafka
         |
         +-- Application events
         +-- Interview events
         +-- Notification events

---

# 3. Service Information

Service Name:

    interview-service


Package:

    com.pi.interview


Port:

    8086


Database:

    interview_db


Database:

    PostgreSQL


Main responsibility:

    Interview scheduling and management

---

# 4. Responsibilities

Interview Service owns:

    Interview

    Interviewer

    Interview slots

    Availability

    Scheduling

    Rescheduling

    Cancellation

    Interview status

    Interview round

    Meeting link

    Interview timestamps

    Interview history


Interview Service communicates with:

    Application Service

    User Service

    Notification Service


It may communicate with:

    Calendar Service

in the future.

---

# 5. What Interview Service Does NOT Own

Interview Service must NOT own:

    Candidate password

    Candidate authentication

    Candidate profile

    Job information

    Application status as a whole

    Application database

    Job database


Ownership:

    Auth Service
        |
        +-- Login
        +-- Password
        +-- JWT


    User Service
        |
        +-- Candidate profile


    Job Service
        |
        +-- Job


    Application Service
        |
        +-- Job application
        +-- Application status


    Interview Service
        |
        +-- Interview
        +-- Slots
        +-- Scheduling


    Notification Service
        |
        +-- Email
        +-- SMS
        +-- Notifications

---

# 6. Core Interview Flow

The main flow is:

    Candidate applies
          |
          v
    APPLIED
          |
          v
    UNDER_REVIEW
          |
          v
    SHORTLISTED
          |
          v
    Interview Service
          |
          v
    Get available slots
          |
          v
    Candidate selects slot
          |
          v
    Book slot
          |
          v
    Interview created
          |
          v
    INTERVIEW_SCHEDULED
          |
          v
    Interview takes place
          |
          v
    Interview completed
          |
          v
    Admin decides
       /       \
      v         v
    HIRED     REJECTED

---

# 7. Important Service Boundary

Application Service owns:

    ApplicationStatus


Interview Service owns:

    InterviewStatus


Do not confuse these.

Application:

    SHORTLISTED

means:

    Candidate is eligible to schedule an interview.


Interview:

    SCHEDULED

means:

    An interview has been booked.


The Application Service eventually becomes:

    INTERVIEW_SCHEDULED


after the Interview Service successfully schedules an interview.

---

# 8. Interview Lifecycle

Recommended interview lifecycle:

    DRAFT
       |
       v
    SCHEDULED
       |
       v
    CONFIRMED
       |
       v
    IN_PROGRESS
       |
       v
    COMPLETED


Other possible states:

    CANCELLED

    RESCHEDULED


However, `RESCHEDULED` is better represented as a history/action rather than a permanent state.

Recommended final status values:

    SCHEDULED

    CONFIRMED

    IN_PROGRESS

    COMPLETED

    CANCELLED

    NO_SHOW


---

# 9. InterviewStatus Enum

Create:

    src/main/java/com/pi/interview/enums/InterviewStatus.java


Code:

~~~java
package com.pi.interview.enums;

public enum InterviewStatus {

    SCHEDULED,

    CONFIRMED,

    IN_PROGRESS,

    COMPLETED,

    CANCELLED,

    NO_SHOW
}
~~~

---

# 10. Interview Type

The platform may support:

    VIDEO

    PHONE

    ONSITE


Create:

    InterviewType.java


Code:

~~~java
package com.pi.interview.enums;

public enum InterviewType {

    VIDEO,

    PHONE,

    ONSITE
}
~~~

---

# 11. Interview Round

A recruitment process can have multiple rounds.

Example:

    Round 1
        Technical Interview

    Round 2
        Managerial Interview

    Round 3
        HR Interview


For MVP, support:

    roundNumber

and:

    roundName


Example:

    roundNumber = 1

    roundName = "Technical Interview"


Later:

    roundNumber = 2

    roundName = "Managerial Interview"


---

# 12. Interview Slot Concept

A slot represents an available time period for an interviewer.

Example:

    Interviewer:
        Rahul

    Date:
        15 September 2026

    Start:
        10:00

    End:
        10:30


This is a slot.


Initially:

    AVAILABLE


After candidate books it:

    BOOKED


Therefore a slot has a lifecycle.

---

# 13. Slot Status

Create:

    SlotStatus.java


Code:

~~~java
package com.pi.interview.enums;

public enum SlotStatus {

    AVAILABLE,

    BOOKED,

    BLOCKED,

    EXPIRED
}
~~~

---

# 14. Why Slots Are Important

Without slots, candidates could choose arbitrary times.

For example:

    Candidate:
        I want interview at 3:17 PM


This creates scheduling problems.

Instead, admin/interviewer creates:

    10:00 - 10:30
    10:30 - 11:00
    11:00 - 11:30
    11:30 - 12:00


Candidate sees only available slots.

This makes scheduling controlled and predictable.

---

# 15. Slot Ownership

Interview Service owns slots.

Example:

    Interview Service

        |
        +-- interviewer
        |
        +-- availability
        |
        +-- slot
        |
        +-- interview


Application Service should never create interview slots.

---

# 16. Database Design

Interview Service owns:

    interview_db


Recommended tables:

    interviewers

    interview_slots

    interviews

    interview_history


Relationship:

    interviewer
        |
        +---- slots
                 |
                 +---- interview


---

# 17. Interviewers Table

Create:

    V1__create_interviewers_table.sql


Code:

~~~sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE interviewers (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    name VARCHAR(255) NOT NULL,

    email VARCHAR(255) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interviewers_user_id
    ON interviewers(user_id);

CREATE INDEX idx_interviewers_active
    ON interviewers(active);
~~~

---

# 18. Why Interviewer Is Stored Separately

The interviewer is a person/user.

But Interview Service needs scheduling information.

Example:

    interviewerId

    active

    availability

    assigned interviews


User Service remains the source of truth for the complete user profile.

Interview Service can maintain an interviewer projection/reference.

Do not duplicate the complete user profile.

---

# 19. Interview Slots Table

Create:

    V2__create_interview_slots_table.sql


Code:

~~~sql
CREATE TABLE interview_slots (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    interviewer_id UUID NOT NULL,

    start_time TIMESTAMP NOT NULL,

    end_time TIMESTAMP NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_slot_time
        CHECK (end_time > start_time)
);

CREATE INDEX idx_slots_interviewer
    ON interview_slots(interviewer_id);

CREATE INDEX idx_slots_start_time
    ON interview_slots(start_time);

CREATE INDEX idx_slots_status
    ON interview_slots(status);
~~~

---

# 20. Interview Table

Create:

    V3__create_interviews_table.sql


Code:

~~~sql
CREATE TABLE interviews (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    application_id UUID NOT NULL,

    candidate_id UUID NOT NULL,

    job_id UUID NOT NULL,

    interviewer_id UUID NOT NULL,

    slot_id UUID NOT NULL,

    round_number INTEGER NOT NULL DEFAULT 1,

    round_name VARCHAR(255) NOT NULL,

    interview_type VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    meeting_link VARCHAR(1000),

    scheduled_start_time TIMESTAMP NOT NULL,

    scheduled_end_time TIMESTAMP NOT NULL,

    notes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_interview_slot
        UNIQUE(slot_id)
);

CREATE INDEX idx_interviews_application
    ON interviews(application_id);

CREATE INDEX idx_interviews_candidate
    ON interviews(candidate_id);

CREATE INDEX idx_interviews_job
    ON interviews(job_id);

CREATE INDEX idx_interviews_interviewer
    ON interviews(interviewer_id);

CREATE INDEX idx_interviews_status
    ON interviews(status);

CREATE INDEX idx_interviews_start_time
    ON interviews(scheduled_start_time);
~~~

---

# 21. Why slot_id Is Unique

Suppose:

    Slot ID = SLOT-100


Candidate A requests it.

Candidate B requests it at the same time.


Without protection:

    Candidate A -> SLOT-100
    Candidate B -> SLOT-100


Both could get the same interview slot.


The database constraint:

~~~sql
UNIQUE(slot_id)
~~~

prevents this.


But we also need row locking.

---

# 22. Interview History Table

Create:

    V4__create_interview_history_table.sql


Code:

~~~sql
CREATE TABLE interview_history (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    interview_id UUID NOT NULL,

    action VARCHAR(50) NOT NULL,

    old_start_time TIMESTAMP,

    old_end_time TIMESTAMP,

    new_start_time TIMESTAMP,

    new_end_time TIMESTAMP,

    old_status VARCHAR(30),

    new_status VARCHAR(30),

    performed_by UUID,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interview_history_interview
    ON interview_history(interview_id);

CREATE INDEX idx_interview_history_created_at
    ON interview_history(created_at);
~~~

---

# 23. Why Interview History Is Needed

Suppose:

    Interview:
        10:00 AM


Admin reschedules:

    2:00 PM


Then later:

    4:00 PM


The current interview record only shows:

    4:00 PM


But the company may need to know:

    Originally:
        10:00 AM

    Rescheduled:
        2:00 PM

    Rescheduled again:
        4:00 PM


Interview history provides this audit trail.

---

# 24. Interviewer Entity

Create:

    Interviewer.java


Code:

~~~java
package com.pi.interview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interviewers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
~~~

---

# 25. InterviewSlot Entity

Create:

    InterviewSlot.java


Code:

~~~java
package com.pi.interview.entity;

import com.pi.interview.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "interviewer_id", nullable = false)
    private UUID interviewerId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SlotStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = SlotStatus.AVAILABLE;
        }
    }

    @PreUpdate
    void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
~~~

---

# 26. Interview Entity

Create:

    Interview.java


Code:

~~~java
package com.pi.interview.entity;

import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.InterviewType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "interviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interview_slot",
                        columnNames = "slot_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "interviewer_id", nullable = false)
    private UUID interviewerId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "round_name", nullable = false)
    private String roundName;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false)
    private InterviewType interviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time", nullable = false)
    private LocalDateTime scheduledEndTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = InterviewStatus.SCHEDULED;
        }
    }

    @PreUpdate
    void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
~~~

---

# 27. Interview History Entity

Create:

    InterviewHistory.java


Code:

~~~java
package com.pi.interview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(nullable = false)
    private String action;

    private LocalDateTime oldStartTime;

    private LocalDateTime oldEndTime;

    private LocalDateTime newStartTime;

    private LocalDateTime newEndTime;

    private String oldStatus;

    private String newStatus;

    private UUID performedBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {

        createdAt = LocalDateTime.now();
    }
}
~~~

---

# 28. Repository

Create:

    InterviewerRepository.java


Code:

~~~java
package com.pi.interview.repository;

import com.pi.interview.entity.Interviewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewerRepository
        extends JpaRepository<Interviewer, UUID> {

    List<Interviewer> findByActiveTrue();
}
~~~

---

# 29. Slot Repository

Create:

    InterviewSlotRepository.java


Use pessimistic locking when booking.

Code:

~~~java
package com.pi.interview.repository;

import com.pi.interview.entity.InterviewSlot;
import com.pi.interview.enums.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSlotRepository
        extends JpaRepository<InterviewSlot, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM InterviewSlot s
        WHERE s.id = :slotId
    """)
    Optional<InterviewSlot> findByIdForUpdate(
            @Param("slotId") UUID slotId
    );

    List<InterviewSlot> findByStatus(
            SlotStatus status
    );

    List<InterviewSlot> findByInterviewerIdAndStatus(
            UUID interviewerId,
            SlotStatus status
    );
}
~~~

---

# 30. Why Pessimistic Locking Is Needed

Suppose two candidates click:

    Book 10:00 AM


at exactly the same time.

Without locking:

    Request A:
        Slot AVAILABLE

    Request B:
        Slot AVAILABLE

Both could attempt booking.


With:

    PESSIMISTIC_WRITE


database behavior becomes:

    Request A
       |
       v
    LOCK SLOT
       |
       v
    BOOK SLOT
       |
       v
    COMMIT
       |
       v
    RELEASE LOCK


Then Request B obtains the lock and sees:

    SLOT = BOOKED


Request B gets:

    409 CONFLICT


This is one of the most important parts of Interview Service.


---

# 31. Interview Repository

Create:

    InterviewRepository.java


Code:

~~~java
package com.pi.interview.repository;

import com.pi.interview.entity.Interview;
import com.pi.interview.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterviewRepository
        extends JpaRepository<Interview, UUID> {

    Page<Interview> findByCandidateId(
            UUID candidateId,
            Pageable pageable
    );

    Page<Interview> findByInterviewerId(
            UUID interviewerId,
            Pageable pageable
    );

    Page<Interview> findByJobId(
            UUID jobId,
            Pageable pageable
    );

    Page<Interview> findByStatus(
            InterviewStatus status,
            Pageable pageable
    );

    boolean existsByApplicationIdAndStatusNot(
            UUID applicationId,
            InterviewStatus status
    );
}
~~~

---

# 32. Interview History Repository

Create:

    InterviewHistoryRepository.java


Code:

~~~java
package com.pi.interview.repository;

import com.pi.interview.entity.InterviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewHistoryRepository
        extends JpaRepository<InterviewHistory, UUID> {

    List<InterviewHistory>
    findByInterviewIdOrderByCreatedAtAsc(
            UUID interviewId
    );
}
~~~

---

# 33. DTO Design

Create these DTOs:

    CreateSlotRequest

    SlotResponse

    ScheduleInterviewRequest

    InterviewResponse

    RescheduleInterviewRequest

    UpdateInterviewStatusRequest

    CreateInterviewerRequest

    InterviewHistoryResponse


---

# 34. CreateSlotRequest

Create:

    CreateSlotRequest.java


Code:

~~~java
package com.pi.interview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateSlotRequest(

        @NotNull
        UUID interviewerId,

        @NotNull
        @Future
        LocalDateTime startTime,

        @NotNull
        @Future
        LocalDateTime endTime

) {
}
~~~

---

# 35. SlotResponse

Create:

    SlotResponse.java


Code:

~~~java
package com.pi.interview.dto;

import com.pi.interview.enums.SlotStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotResponse(

        UUID slotId,

        UUID interviewerId,

        LocalDateTime startTime,

        LocalDateTime endTime,

        SlotStatus status

) {
}
~~~

---

# 36. ScheduleInterviewRequest

Candidate should NOT send:

    candidateId


Candidate identity comes from JWT.


Create:

    ScheduleInterviewRequest.java


Code:

~~~java
package com.pi.interview.dto;

import com.pi.interview.enums.InterviewType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ScheduleInterviewRequest(

        @NotNull
        UUID applicationId,

        @NotNull
        UUID slotId,

        @NotNull
        Integer roundNumber,

        @NotNull
        @Size(max = 255)
        String roundName,

        @NotNull
        InterviewType interviewType

) {
}
~~~

---

# 37. InterviewResponse

Create:

    InterviewResponse.java


Code:

~~~java
package com.pi.interview.dto;

import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.InterviewType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterviewResponse(

        UUID interviewId,

        UUID applicationId,

        UUID candidateId,

        UUID jobId,

        UUID interviewerId,

        UUID slotId,

        Integer roundNumber,

        String roundName,

        InterviewType interviewType,

        InterviewStatus status,

        String meetingLink,

        LocalDateTime scheduledStartTime,

        LocalDateTime scheduledEndTime,

        String notes,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}
~~~

---

# 38. RescheduleInterviewRequest

Create:

    RescheduleInterviewRequest.java


Code:

~~~java
package com.pi.interview.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RescheduleInterviewRequest(

        @NotNull
        UUID newSlotId

) {
}
~~~

---

# 39. UpdateInterviewStatusRequest

Create:

    UpdateInterviewStatusRequest.java


Code:

~~~java
package com.pi.interview.dto;

import com.pi.interview.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewStatusRequest(

        @NotNull
        InterviewStatus status,

        String notes

) {
}
~~~

---

# 40. Interview Service Interface

Create:

    InterviewService.java


Code:

~~~java
package com.pi.interview.service;

import com.pi.interview.dto.*;
import com.pi.interview.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InterviewService {

    SlotResponse createSlot(
            CreateSlotRequest request
    );

    Page<SlotResponse> getAvailableSlots(
            Pageable pageable
    );

    Page<SlotResponse> getAvailableSlotsForApplication(
            UUID applicationId,
            Pageable pageable
    );

    InterviewResponse scheduleInterview(
            ScheduleInterviewRequest request,
            UUID candidateId
    );

    InterviewResponse getInterview(
            UUID interviewId,
            UUID candidateId
    );

    Page<InterviewResponse> getMyInterviews(
            UUID candidateId,
            Pageable pageable
    );

    Page<InterviewResponse> getAllInterviews(
            Pageable pageable
    );

    InterviewResponse rescheduleInterview(
            UUID interviewId,
            UUID newSlotId,
            UUID adminId
    );

    InterviewResponse updateStatus(
            UUID interviewId,
            InterviewStatus newStatus,
            String notes,
            UUID actorId
    );

    InterviewResponse cancelInterview(
            UUID interviewId,
            UUID actorId
    );
}
~~~

---

# 41. Application Service Client

Interview Service must verify:

    Application exists

    Candidate owns application

    Application belongs to requested candidate

    Application status = SHORTLISTED


It should call Application Service.

Create:

    ApplicationServiceClient.java


Code:

~~~java
package com.pi.interview.client;

import com.pi.interview.enums.ApplicationStatus;

import java.util.UUID;

public interface ApplicationServiceClient {

    ApplicationEligibility getInterviewEligibility(
            UUID applicationId
    );

    record ApplicationEligibility(

            UUID applicationId,

            UUID candidateId,

            UUID jobId,

            ApplicationStatus status,

            boolean eligibleForInterview

    ) {
    }
}
~~~

---

# 42. Why Interview Service Calls Application Service

Suppose a candidate tries:

    POST /api/v1/interviews


with:

    applicationId = APPLICATION-123


The Interview Service cannot simply trust it.

It calls:

    Application Service


Application Service says:

    candidateId = CANDIDATE-100

    jobId = JOB-500

    status = SHORTLISTED

    eligibleForInterview = true


Only then can Interview Service schedule the interview.

---

# 43. Important Security Check

Suppose Candidate A tries to schedule Candidate B's application.

Candidate A's JWT:

    candidateId = A


Application:

    candidateId = B


Application Service returns:

    candidateId = B


Interview Service compares:

    A != B


Request is rejected.


This is defense in depth.


---

# 44. Interview Service Implementation

Create:

    InterviewServiceImpl.java


Code:

~~~java
package com.pi.interview.service;

import com.pi.interview.client.ApplicationServiceClient;
import com.pi.interview.dto.*;
import com.pi.interview.entity.Interview;
import com.pi.interview.entity.InterviewHistory;
import com.pi.interview.entity.InterviewSlot;
import com.pi.interview.enums.ApplicationStatus;
import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.SlotStatus;
import com.pi.interview.exception.*;
import com.pi.interview.mapper.InterviewMapper;
import com.pi.interview.repository.InterviewHistoryRepository;
import com.pi.interview.repository.InterviewRepository;
import com.pi.interview.repository.InterviewSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewServiceImpl
        implements InterviewService {

    private final InterviewRepository interviewRepository;

    private final InterviewSlotRepository slotRepository;

    private final InterviewHistoryRepository
            historyRepository;

    private final ApplicationServiceClient
            applicationServiceClient;

    private final InterviewMapper interviewMapper;


    @Override
    public SlotResponse createSlot(
            CreateSlotRequest request
    ) {

        if (!request.endTime()
                .isAfter(request.startTime())) {

            throw new InvalidSlotException(
                    "End time must be after start time"
            );
        }


        InterviewSlot slot =
                InterviewSlot.builder()
                        .interviewerId(
                                request.interviewerId()
                        )
                        .startTime(
                                request.startTime()
                        )
                        .endTime(
                                request.endTime()
                        )
                        .status(
                                SlotStatus.AVAILABLE
                        )
                        .build();


        InterviewSlot saved =
                slotRepository.save(slot);


        return new SlotResponse(
                saved.getId(),
                saved.getInterviewerId(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getStatus()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SlotResponse>
    getAvailableSlots(
            Pageable pageable
    ) {

        return slotRepository
                .findAll(pageable)
                .map(slot ->
                        new SlotResponse(
                                slot.getId(),
                                slot.getInterviewerId(),
                                slot.getStartTime(),
                                slot.getEndTime(),
                                slot.getStatus()
                        )
                );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SlotResponse>
    getAvailableSlotsForApplication(
            UUID applicationId,
            Pageable pageable
    ) {

        ApplicationServiceClient.ApplicationEligibility
                eligibility =
                applicationServiceClient
                        .getInterviewEligibility(
                                applicationId
                        );


        if (!eligibility.eligibleForInterview()) {

            throw new InterviewNotAllowedException(
                    "Candidate is not eligible for interview"
            );
        }


        return slotRepository
                .findByStatus(
                        SlotStatus.AVAILABLE
                )
                .stream()
                .filter(slot ->
                        slot.getStartTime()
                                .isAfter(
                                        LocalDateTime.now()
                                )
                )
                .map(slot ->
                        new SlotResponse(
                                slot.getId(),
                                slot.getInterviewerId(),
                                slot.getStartTime(),
                                slot.getEndTime(),
                                slot.getStatus()
                        )
                )
                .skip(
                        pageable.getOffset()
                )
                .limit(
                        pageable.getPageSize()
                )
                .collect(
                        java.util.stream.Collectors
                                .collectingAndThen(
                                        java.util.stream.Collectors
                                                .toList(),
                                        list ->
                                                new org.springframework.data.domain.PageImpl<>(
                                                        list,
                                                        pageable,
                                                        list.size()
                                                )
                                )
                );
    }


    @Override
    public InterviewResponse scheduleInterview(
            ScheduleInterviewRequest request,
            UUID candidateId
    ) {

        // -----------------------------------------------
        // 1. Validate application
        // -----------------------------------------------

        ApplicationServiceClient.ApplicationEligibility
                eligibility =
                applicationServiceClient
                        .getInterviewEligibility(
                                request.applicationId()
                        );


        // -----------------------------------------------
        // 2. Validate candidate ownership
        // -----------------------------------------------

        if (!eligibility.candidateId()
                .equals(candidateId)) {

            throw new InterviewAccessDeniedException(
                    "Application does not belong to candidate"
            );
        }


        // -----------------------------------------------
        // 3. Validate application status
        // -----------------------------------------------

        if (!eligibility.eligibleForInterview()
                || eligibility.status()
                != ApplicationStatus.SHORTLISTED) {

            throw new InterviewNotAllowedException(
                    "Application is not eligible for interview"
            );
        }


        // -----------------------------------------------
        // 4. Lock slot
        // -----------------------------------------------

        InterviewSlot slot =
                slotRepository
                        .findByIdForUpdate(
                                request.slotId()
                        )
                        .orElseThrow(() ->
                                new SlotNotFoundException(
                                        "Interview slot not found"
                                )
                        );


        // -----------------------------------------------
        // 5. Check slot status
        // -----------------------------------------------

        if (slot.getStatus()
                != SlotStatus.AVAILABLE) {

            throw new SlotAlreadyBookedException(
                    "Interview slot is no longer available"
            );
        }


        // -----------------------------------------------
        // 6. Check slot time
        // -----------------------------------------------

        if (!slot.getStartTime()
                .isAfter(LocalDateTime.now())) {

            slot.setStatus(
                    SlotStatus.EXPIRED
            );

            throw new SlotExpiredException(
                    "Interview slot has expired"
            );
        }


        // -----------------------------------------------
        // 7. Check existing interview
        // -----------------------------------------------

        if (interviewRepository
                .existsByApplicationIdAndStatusNot(
                        request.applicationId(),
                        InterviewStatus.CANCELLED
                )) {

            throw new InterviewAlreadyExistsException(
                    "Active interview already exists for application"
            );
        }


        // -----------------------------------------------
        // 8. Book slot
        // -----------------------------------------------

        slot.setStatus(
                SlotStatus.BOOKED
        );


        // -----------------------------------------------
        // 9. Create interview
        // -----------------------------------------------

        Interview interview =
                Interview.builder()
                        .applicationId(
                                request.applicationId()
                        )
                        .candidateId(
                                candidateId
                        )
                        .jobId(
                                eligibility.jobId()
                        )
                        .interviewerId(
                                slot.getInterviewerId()
                        )
                        .slotId(
                                slot.getId()
                        )
                        .roundNumber(
                                request.roundNumber()
                        )
                        .roundName(
                                request.roundName()
                        )
                        .interviewType(
                                request.interviewType()
                        )
                        .status(
                                InterviewStatus.SCHEDULED
                        )
                        .scheduledStartTime(
                                slot.getStartTime()
                        )
                        .scheduledEndTime(
                                slot.getEndTime()
                        )
                        .build();


        Interview saved =
                interviewRepository.save(
                        interview
                );


        // -----------------------------------------------
        // 10. History
        // -----------------------------------------------

        InterviewHistory history =
                InterviewHistory.builder()
                        .interviewId(
                                saved.getId()
                        )
                        .action(
                                "SCHEDULED"
                        )
                        .newStartTime(
                                saved.getScheduledStartTime()
                        )
                        .newEndTime(
                                saved.getScheduledEndTime()
                        )
                        .newStatus(
                                InterviewStatus.SCHEDULED
                                        .name()
                        )
                        .performedBy(
                                candidateId
                        )
                        .remarks(
                                "Interview scheduled"
                        )
                        .build();


        historyRepository.save(history);


        return interviewMapper.toResponse(
                saved
        );
    }


    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterview(
            UUID interviewId,
            UUID candidateId
    ) {

        Interview interview =
                interviewRepository.findById(
                                interviewId
                        )
                        .orElseThrow(() ->
                                new InterviewNotFoundException(
                                        "Interview not found"
                                )
                        );


        if (!interview.getCandidateId()
                .equals(candidateId)) {

            throw new InterviewAccessDeniedException(
                    "You cannot access this interview"
            );
        }


        return interviewMapper.toResponse(
                interview
        );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse>
    getMyInterviews(
            UUID candidateId,
            Pageable pageable
    ) {

        return interviewRepository
                .findByCandidateId(
                        candidateId,
                        pageable
                )
                .map(interviewMapper::toResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse>
    getAllInterviews(
            Pageable pageable
    ) {

        return interviewRepository
                .findAll(pageable)
                .map(interviewMapper::toResponse);
    }


    @Override
    public InterviewResponse rescheduleInterview(
            UUID interviewId,
            UUID newSlotId,
            UUID adminId
    ) {

        Interview interview =
                interviewRepository.findById(
                                interviewId
                        )
                        .orElseThrow(() ->
                                new InterviewNotFoundException(
                                        "Interview not found"
                                )
                        );


        if (interview.getStatus()
                == InterviewStatus.CANCELLED
                || interview.getStatus()
                == InterviewStatus.COMPLETED) {

            throw new InvalidInterviewStateException(
                    "Interview cannot be rescheduled"
            );
        }


        InterviewSlot newSlot =
                slotRepository
                        .findByIdForUpdate(
                                newSlotId
                        )
                        .orElseThrow(() ->
                                new SlotNotFoundException(
                                        "New slot not found"
                                )
                        );


        if (newSlot.getStatus()
                != SlotStatus.AVAILABLE) {

            throw new SlotAlreadyBookedException(
                    "New slot is not available"
            );
        }


        if (!newSlot.getStartTime()
                .isAfter(LocalDateTime.now())) {

            throw new SlotExpiredException(
                    "New slot has already started"
            );
        }


        InterviewSlot oldSlot =
                slotRepository
                        .findByIdForUpdate(
                                interview.getSlotId()
                        )
                        .orElseThrow(() ->
                                new SlotNotFoundException(
                                        "Old slot not found"
                                )
                        );


        LocalDateTime oldStart =
                interview.getScheduledStartTime();

        LocalDateTime oldEnd =
                interview.getScheduledEndTime();


        oldSlot.setStatus(
                SlotStatus.AVAILABLE
        );


        newSlot.setStatus(
                SlotStatus.BOOKED
        );


        interview.setSlotId(
                newSlot.getId()
        );

        interview.setInterviewerId(
                newSlot.getInterviewerId()
        );

        interview.setScheduledStartTime(
                newSlot.getStartTime()
        );

        interview.setScheduledEndTime(
                newSlot.getEndTime()
        );


        InterviewHistory history =
                InterviewHistory.builder()
                        .interviewId(
                                interview.getId()
                        )
                        .action(
                                "RESCHEDULED"
                        )
                        .oldStartTime(oldStart)
                        .oldEndTime(oldEnd)
                        .newStartTime(
                                newSlot.getStartTime()
                        )
                        .newEndTime(
                                newSlot.getEndTime()
                        )
                        .oldStatus(
                                interview.getStatus()
                                        .name()
                        )
                        .newStatus(
                                interview.getStatus()
                                        .name()
                        )
                        .performedBy(adminId)
                        .remarks(
                                "Interview rescheduled by admin"
                        )
                        .build();


        historyRepository.save(history);


        return interviewMapper.toResponse(
                interview
        );
    }


    @Override
    public InterviewResponse updateStatus(
            UUID interviewId,
            InterviewStatus newStatus,
            String notes,
            UUID actorId
    ) {

        Interview interview =
                interviewRepository.findById(
                                interviewId
                        )
                        .orElseThrow(() ->
                                new InterviewNotFoundException(
                                        "Interview not found"
                                )
                        );


        InterviewStatus current =
                interview.getStatus();


        validateStatusTransition(
                current,
                newStatus
        );


        interview.setStatus(
                newStatus
        );


        if (notes != null) {
            interview.setNotes(notes);
        }


        InterviewHistory history =
                InterviewHistory.builder()
                        .interviewId(
                                interview.getId()
                        )
                        .action(
                                "STATUS_CHANGED"
                        )
                        .oldStatus(
                                current.name()
                        )
                        .newStatus(
                                newStatus.name()
                        )
                        .performedBy(actorId)
                        .remarks(notes)
                        .build();


        historyRepository.save(history);


        return interviewMapper.toResponse(
                interview
        );
    }


    @Override
    public InterviewResponse cancelInterview(
            UUID interviewId,
            UUID actorId
    ) {

        return updateStatus(
                interviewId,
                InterviewStatus.CANCELLED,
                "Interview cancelled",
                actorId
        );
    }


    private void validateStatusTransition(
            InterviewStatus current,
            InterviewStatus next
    ) {

        boolean valid = switch (current) {

            case SCHEDULED ->
                    next == InterviewStatus.CONFIRMED
                            || next == InterviewStatus.CANCELLED
                            || next == InterviewStatus.NO_SHOW;

            case CONFIRMED ->
                    next == InterviewStatus.IN_PROGRESS
                            || next == InterviewStatus.CANCELLED
                            || next == InterviewStatus.NO_SHOW;

            case IN_PROGRESS ->
                    next == InterviewStatus.COMPLETED
                            || next == InterviewStatus.CANCELLED;

            case COMPLETED,
                 CANCELLED,
                 NO_SHOW ->
                    false;
        };


        if (!valid) {

            throw new InvalidInterviewStateException(
                    "Invalid interview status transition: "
                            + current
                            + " -> "
                            + next
            );
        }
    }
}
~~~

---

# 45. Interview Mapper

Create:

    InterviewMapper.java


Code:

~~~java
package com.pi.interview.mapper;

import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.entity.Interview;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewResponse toResponse(
            Interview interview
    ) {

        return new InterviewResponse(
                interview.getId(),
                interview.getApplicationId(),
                interview.getCandidateId(),
                interview.getJobId(),
                interview.getInterviewerId(),
                interview.getSlotId(),
                interview.getRoundNumber(),
                interview.getRoundName(),
                interview.getInterviewType(),
                interview.getStatus(),
                interview.getMeetingLink(),
                interview.getScheduledStartTime(),
                interview.getScheduledEndTime(),
                interview.getNotes(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }
}
~~~

---

# 46. Candidate Interview Controller

Create:

    InterviewController.java


Code:

~~~java
package com.pi.interview.controller;

import com.pi.interview.dto.*;
import com.pi.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;


    @GetMapping("/slots")
    public Page<SlotResponse>
    getAvailableSlots(
            @RequestParam UUID applicationId,
            Pageable pageable
    ) {

        return interviewService
                .getAvailableSlotsForApplication(
                        applicationId,
                        pageable
                );
    }


    @PostMapping
    public ResponseEntity<InterviewResponse>
    scheduleInterview(
            @Valid
            @RequestBody
            ScheduleInterviewRequest request,

            Authentication authentication
    ) {

        UUID candidateId =
                UUID.fromString(
                        authentication.getName()
                );


        InterviewResponse response =
                interviewService.scheduleInterview(
                        request,
                        candidateId
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping("/me")
    public Page<InterviewResponse>
    getMyInterviews(
            Pageable pageable,
            Authentication authentication
    ) {

        UUID candidateId =
                UUID.fromString(
                        authentication.getName()
                );


        return interviewService
                .getMyInterviews(
                        candidateId,
                        pageable
                );
    }


    @GetMapping("/{interviewId}")
    public InterviewResponse getInterview(
            @PathVariable UUID interviewId,
            Authentication authentication
    ) {

        UUID candidateId =
                UUID.fromString(
                        authentication.getName()
                );


        return interviewService
                .getInterview(
                        interviewId,
                        candidateId
                );
    }
}
~~~

---

# 47. Candidate APIs

Candidate endpoints:

    GET
    /api/v1/interviews/slots?applicationId={applicationId}


    POST
    /api/v1/interviews


    GET
    /api/v1/interviews/me


    GET
    /api/v1/interviews/{interviewId}


---

# 48. Get Available Slots

Candidate has:

    applicationId = APP-100


Candidate requests:

    GET /api/v1/interviews/slots?applicationId=APP-100


Interview Service:

    1. Calls Application Service

    2. Checks application

    3. Checks candidate eligibility

    4. Gets available slots

    5. Returns slots


Example response:

~~~json
{
    "content": [
        {
            "slotId": "slot-100",
            "interviewerId": "interviewer-10",
            "startTime": "2026-09-15T10:00:00",
            "endTime": "2026-09-15T10:30:00",
            "status": "AVAILABLE"
        },
        {
            "slotId": "slot-101",
            "interviewerId": "interviewer-11",
            "startTime": "2026-09-15T11:00:00",
            "endTime": "2026-09-15T11:30:00",
            "status": "AVAILABLE"
        }
    ]
}
~~~

---

# 49. Schedule Interview API

Candidate selects:

    slotId = SLOT-100


Request:

    POST /api/v1/interviews


Body:

~~~json
{
    "applicationId": "APP-100",
    "slotId": "SLOT-100",
    "roundNumber": 1,
    "roundName": "Technical Interview",
    "interviewType": "VIDEO"
}
~~~

JWT provides:

    candidateId


The candidate does not send:

    candidateId


---

# 50. Schedule Interview Flow

Detailed flow:

    Candidate
        |
        v
    POST /api/v1/interviews
        |
        v
    API Gateway
        |
        v
    Interview Service
        |
        +---- Get candidateId from JWT
        |
        +---- Call Application Service
        |
        +---- Validate application
        |
        +---- Validate candidate
        |
        +---- Validate SHORTLISTED
        |
        +---- Lock slot
        |
        +---- Check slot AVAILABLE
        |
        +---- Mark slot BOOKED
        |
        +---- Create interview
        |
        +---- Create interview history
        |
        v
    Return InterviewResponse

---

# 51. Example Successful Response

~~~json
{
    "interviewId": "INT-100",
    "applicationId": "APP-100",
    "candidateId": "CAND-100",
    "jobId": "JOB-100",
    "interviewerId": "INTV-100",
    "slotId": "SLOT-100",
    "roundNumber": 1,
    "roundName": "Technical Interview",
    "interviewType": "VIDEO",
    "status": "SCHEDULED",
    "meetingLink": null,
    "scheduledStartTime": "2026-09-15T10:00:00",
    "scheduledEndTime": "2026-09-15T10:30:00",
    "notes": null
}
~~~

---

# 52. Meeting Link

For video interviews, the meeting link can be generated after scheduling.

Examples:

    Google Meet

    Microsoft Teams

    Zoom

    Internal video platform


Initially the field can be:

    null


Later a meeting service can generate:

    meetingLink


Example:

~~~json
{
    "meetingLink": "https://meet.example.com/interview-123"
}
~~~

Do not make Interview Service responsible for implementing a complete video conferencing platform.

---

# 53. Admin Controller

Create:

    AdminInterviewController.java


Code:

~~~java
package com.pi.interview.controller;

import com.pi.interview.dto.*;
import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/interviews")
@RequiredArgsConstructor
public class AdminInterviewController {

    private final InterviewService interviewService;


    @PostMapping("/slots")
    public SlotResponse createSlot(
            @Valid
            @RequestBody
            CreateSlotRequest request
    ) {

        return interviewService.createSlot(
                request
        );
    }


    @GetMapping
    public Page<InterviewResponse>
    getAllInterviews(
            Pageable pageable
    ) {

        return interviewService
                .getAllInterviews(
                        pageable
                );
    }


    @PatchMapping("/{interviewId}/status")
    public InterviewResponse updateStatus(
            @PathVariable UUID interviewId,

            @Valid
            @RequestBody
            UpdateInterviewStatusRequest request,

            Authentication authentication
    ) {

        UUID adminId =
                UUID.fromString(
                        authentication.getName()
                );


        return interviewService.updateStatus(
                interviewId,
                request.status(),
                request.notes(),
                adminId
        );
    }


    @PostMapping("/{interviewId}/reschedule")
    public InterviewResponse reschedule(
            @PathVariable UUID interviewId,

            @Valid
            @RequestBody
            RescheduleInterviewRequest request,

            Authentication authentication
    ) {

        UUID adminId =
                UUID.fromString(
                        authentication.getName()
                );


        return interviewService.rescheduleInterview(
                interviewId,
                request.newSlotId(),
                adminId
        );
    }


    @PostMapping("/{interviewId}/cancel")
    public InterviewResponse cancel(
            @PathVariable UUID interviewId,

            Authentication authentication
    ) {

        UUID adminId =
                UUID.fromString(
                        authentication.getName()
                );


        return interviewService.cancelInterview(
                interviewId,
                adminId
        );
    }
}
~~~

---

# 54. Admin APIs

Admin endpoints:

    POST
    /api/v1/admin/interviews/slots


    GET
    /api/v1/admin/interviews


    PATCH
    /api/v1/admin/interviews/{interviewId}/status


    POST
    /api/v1/admin/interviews/{interviewId}/reschedule


    POST
    /api/v1/admin/interviews/{interviewId}/cancel


---

# 55. Admin Creates Slots

Admin creates:

~~~json
{
    "interviewerId": "INTERVIEWER-100",
    "startTime": "2026-09-15T10:00:00",
    "endTime": "2026-09-15T10:30:00"
}
~~~

Interview Service creates:

    SLOT-100

with:

    AVAILABLE


Admin can create multiple slots:

    10:00 - 10:30

    10:30 - 11:00

    11:00 - 11:30

    11:30 - 12:00

---

# 56. Slot Overlap

The service should prevent an interviewer from having overlapping slots.

Example:

    Existing:
        10:00 - 10:30


New request:

    10:15 - 10:45


This should be rejected.


Later add repository query:

~~~java
@Query("""
    SELECT COUNT(s)
    FROM InterviewSlot s
    WHERE s.interviewerId = :interviewerId
      AND s.status <> 'BLOCKED'
      AND s.startTime < :endTime
      AND s.endTime > :startTime
""")
long countOverlappingSlots(
        UUID interviewerId,
        LocalDateTime startTime,
        LocalDateTime endTime
);
~~~

Before creating a slot:

    countOverlappingSlots > 0

means:

    reject request


This is important for real scheduling.


---

# 57. Interview Rescheduling

Suppose interview is:

    September 15
    10:00 AM


Admin wants:

    September 16
    3:00 PM


Admin selects another available slot.

Flow:

    Existing Interview
          |
          v
    Select New Slot
          |
          v
    Lock New Slot
          |
          v
    Check AVAILABLE
          |
          v
    Release Old Slot
          |
          v
    Book New Slot
          |
          v
    Update Interview
          |
          v
    Create History
          |
          v
    Notify Candidate


---

# 58. Rescheduling Rules

Allowed:

    SCHEDULED -> reschedule

    CONFIRMED -> reschedule


Not allowed:

    COMPLETED

    CANCELLED

    NO_SHOW


Once an interview has been completed, changing its scheduled time makes no sense.

---

# 59. Why Rescheduling Is Admin-Controlled

Candidate may request a reschedule in a future version.

For the MVP:

    Admin controls rescheduling.


Reason:

    Company controls interviewer availability.

    Company may need to coordinate interviewers.

    Company may have internal scheduling policies.


Later add:

    Candidate requests reschedule

and:

    Admin approves request.

---

# 60. Interview Cancellation

Admin can cancel:

    SCHEDULED

    CONFIRMED

    IN_PROGRESS

depending on company policy.


Recommended MVP:

    SCHEDULED -> CANCELLED

    CONFIRMED -> CANCELLED


When cancelled:

    Interview status = CANCELLED

    Slot = AVAILABLE


unless the company intentionally wants the slot blocked.

---

# 61. Candidate Cancel

For the first version, candidate cancellation can be disabled.

Later:

    POST /api/v1/interviews/{id}/cancel


But if candidate cancellation is added, business rules should be introduced.

For example:

    Candidate can cancel
    more than 24 hours before interview.


This is a business requirement, not a technical requirement.


---

# 62. Interview Status Updates

Example:

    SCHEDULED
         |
         v
    CONFIRMED
         |
         v
    IN_PROGRESS
         |
         v
    COMPLETED


If candidate doesn't attend:

    CONFIRMED
         |
         v
    NO_SHOW


If admin cancels:

    SCHEDULED
         |
         v
    CANCELLED


---

# 63. Admin Status API

Request:

    PATCH /api/v1/admin/interviews/{id}/status


Body:

~~~json
{
    "status": "COMPLETED",
    "notes": "Candidate completed the technical interview."
}
~~~

This updates:

    Interview status


and creates:

    Interview history


---

# 64. Candidate My Interviews

Endpoint:

    GET /api/v1/interviews/me


Candidate receives only their own interviews.

Example:

~~~json
{
    "content": [
        {
            "interviewId": "INT-100",
            "applicationId": "APP-100",
            "jobId": "JOB-100",
            "roundName": "Technical Interview",
            "status": "SCHEDULED",
            "scheduledStartTime": "2026-09-15T10:00:00",
            "scheduledEndTime": "2026-09-15T10:30:00"
        }
    ]
}
~~~

---

# 65. Candidate Interview Details

Endpoint:

    GET /api/v1/interviews/{interviewId}


The service checks:

    JWT candidateId

against:

    interview.candidateId


If different:

    403 FORBIDDEN


---

# 66. Admin Interview Dashboard

Admin dashboard should display:

    Candidate

    Job

    Interviewer

    Interview Round

    Date

    Time

    Type

    Status


Example:

    Candidate:
        Sanchit

    Job:
        Backend Developer

    Interviewer:
        Rahul

    Round:
        Technical

    Time:
        10:00 AM

    Type:
        Video

    Status:
        SCHEDULED

---

# 67. Interviewer Dashboard

Later add:

    GET /api/v1/interviewer/interviews/me


Interviewer should see:

    Today's interviews

    Upcoming interviews

    Completed interviews

    Cancelled interviews


The interviewer identity comes from JWT.

Do not accept arbitrary interviewerId from the frontend.


---

# 68. Interviewer Availability

A more advanced implementation can introduce:

    availability


Example:

    Monday
        09:00 - 17:00

    Tuesday
        09:00 - 17:00


The system can automatically generate slots.

For MVP, manually creating slots is simpler.

Recommended:

    MVP:
        Admin creates slots manually

    V2:
        Recurring interviewer availability

---

# 69. Automatic Slot Generation

Future flow:

    Interviewer defines:

    Monday
    10:00 - 14:00

    Slot duration:
    30 minutes


System creates:

    10:00 - 10:30

    10:30 - 11:00

    11:00 - 11:30

    11:30 - 12:00

    12:00 - 12:30

    12:30 - 13:00

    13:00 - 13:30

    13:30 - 14:00


This can be added later.


---

# 70. Time Zone Design

Interview scheduling is time-sensitive.

Do not assume every user is in the same timezone forever.

For the first version, define:

    timezone = Asia/Kolkata


But the better long-term design is to store timestamps in UTC.

Recommended production approach:

    Database:
        UTC

    API:
        ISO-8601 with timezone

    Frontend:
        Convert to user's timezone


Example:

    2026-09-15T10:00:00+05:30


Later use:

    Instant

or:

    OffsetDateTime


instead of plain LocalDateTime for distributed systems.

If the existing 366PI services consistently use LocalDateTime, keep it for MVP and standardize timezone handling before production.


---

# 71. Interview Security

Security rules:

    Candidate:
        View own interviews
        Schedule own interview
        View available slots


    Admin:
        Create slots
        View all interviews
        Reschedule
        Cancel
        Change status


    Interviewer:
        View assigned interviews
        Update interview progress


    Internal services:
        Communicate through secured service-to-service APIs


---

# 72. SecurityConfig

Create:

    SecurityConfig.java


Example:

~~~java
package com.pi.interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            .csrf(csrf ->
                    csrf.disable()
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        "/actuator/health"
                )
                .permitAll()

                .requestMatchers(
                        "/api/v1/admin/**"
                )
                .hasRole("ADMIN")

                .requestMatchers(
                        "/api/v1/interviews/**"
                )
                .authenticated()

                .requestMatchers(
                        "/internal/**"
                )
                .authenticated()

                .anyRequest()
                .authenticated()
            )

            .oauth2ResourceServer(
                    oauth2 ->
                            oauth2.jwt()
            );

        return http.build();
    }
}
~~~

Adapt the JWT decoder, issuer, and role mapping to the Auth Service implementation used by 366PI.

---

# 73. Internal Service Authentication

Do not expose internal endpoints publicly.

For example:

    /internal/v1/applications/...


should be accessible only to trusted services.

Possible approaches:

    Service JWT

    OAuth2 client credentials

    mTLS


For MVP:

    internal service JWT


For a more mature architecture:

    OAuth2 client credentials
    or mTLS


---

# 74. Exception Classes

Create:

    InterviewNotFoundException.java


~~~java
package com.pi.interview.exception;

public class InterviewNotFoundException
        extends RuntimeException {

    public InterviewNotFoundException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    SlotNotFoundException.java


~~~java
package com.pi.interview.exception;

public class SlotNotFoundException
        extends RuntimeException {

    public SlotNotFoundException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    SlotAlreadyBookedException.java


~~~java
package com.pi.interview.exception;

public class SlotAlreadyBookedException
        extends RuntimeException {

    public SlotAlreadyBookedException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    SlotExpiredException.java


~~~java
package com.pi.interview.exception;

public class SlotExpiredException
        extends RuntimeException {

    public SlotExpiredException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    InterviewNotAllowedException.java


~~~java
package com.pi.interview.exception;

public class InterviewNotAllowedException
        extends RuntimeException {

    public InterviewNotAllowedException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    InterviewAlreadyExistsException.java


~~~java
package com.pi.interview.exception;

public class InterviewAlreadyExistsException
        extends RuntimeException {

    public InterviewAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    InvalidInterviewStateException.java


~~~java
package com.pi.interview.exception;

public class InvalidInterviewStateException
        extends RuntimeException {

    public InvalidInterviewStateException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    InvalidSlotException.java


~~~java
package com.pi.interview.exception;

public class InvalidSlotException
        extends RuntimeException {

    public InvalidSlotException(
            String message
    ) {
        super(message);
    }
}
~~~

Create:

    InterviewAccessDeniedException.java


~~~java
package com.pi.interview.exception;

public class InterviewAccessDeniedException
        extends RuntimeException {

    public InterviewAccessDeniedException(
            String message
    ) {
        super(message);
    }
}
~~~

---

# 75. Global Exception Handler

Create:

    GlobalExceptionHandler.java


Code:

~~~java
package com.pi.interview.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            InterviewNotFoundException.class
    )
    public ResponseEntity<?> handleInterviewNotFound(
            InterviewNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        404,

                        "code",
                        "INTERVIEW_NOT_FOUND",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            SlotNotFoundException.class
    )
    public ResponseEntity<?> handleSlotNotFound(
            SlotNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        404,

                        "code",
                        "SLOT_NOT_FOUND",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            SlotAlreadyBookedException.class
    )
    public ResponseEntity<?> handleSlotBooked(
            SlotAlreadyBookedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        409,

                        "code",
                        "SLOT_ALREADY_BOOKED",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            SlotExpiredException.class
    )
    public ResponseEntity<?> handleSlotExpired(
            SlotExpiredException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        409,

                        "code",
                        "SLOT_EXPIRED",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            InterviewNotAllowedException.class
    )
    public ResponseEntity<?> handleNotAllowed(
            InterviewNotAllowedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        400,

                        "code",
                        "INTERVIEW_NOT_ALLOWED",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            InterviewAlreadyExistsException.class
    )
    public ResponseEntity<?> handleAlreadyExists(
            InterviewAlreadyExistsException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        409,

                        "code",
                        "INTERVIEW_ALREADY_EXISTS",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            InvalidInterviewStateException.class
    )
    public ResponseEntity<?> handleInvalidState(
            InvalidInterviewStateException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        400,

                        "code",
                        "INVALID_INTERVIEW_STATE",

                        "message",
                        ex.getMessage()
                ));
    }


    @ExceptionHandler(
            InterviewAccessDeniedException.class
    )
    public ResponseEntity<?> handleAccessDenied(
            InterviewAccessDeniedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp",
                        LocalDateTime.now(),

                        "status",
                        403,

                        "code",
                        "INTERVIEW_ACCESS_DENIED",

                        "message",
                        ex.getMessage()
                ));
    }
}
~~~

---

# 76. application.yml

Create:

    src/main/resources/application.yml


Code:

~~~yaml
server:
  port: 8086

spring:

  application:
    name: interview-service

  datasource:
    url: jdbc:postgresql://localhost:5432/interview_db
    username: postgres
    password: postgres

  jpa:

    hibernate:
      ddl-auto: validate

    open-in-view: false

    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration


eureka:

  client:

    service-url:
      defaultZone: http://localhost:8761/eureka/

    register-with-eureka: true

    fetch-registry: true


management:

  endpoints:

    web:

      exposure:
        include:
          - health
          - info
          - metrics

  endpoint:

    health:

      show-details: always
~~~

---

# 77. PostgreSQL Database

Create:

    interview_db


Example:

~~~sql
CREATE DATABASE interview_db;
~~~

Then configure:

    username

    password

in:

    application.yml


Do not commit production database passwords into Git.


---

# 78. Flyway Migration Order

Use:

    V1__create_interviewers_table.sql

    V2__create_interview_slots_table.sql

    V3__create_interviews_table.sql

    V4__create_interview_history_table.sql


Future migrations:

    V5__add_timezone.sql

    V6__add_cancellation_reason.sql

    V7__add_interviewer_notes.sql


Never edit migrations that have already been applied in shared environments.


---

# 79. Maven Dependencies

Recommended dependencies:

    Spring Web

    Spring Data JPA

    PostgreSQL Driver

    Flyway

    Validation

    Spring Security

    OAuth2 Resource Server

    Eureka Client

    Actuator

    Springdoc OpenAPI

    Lombok

    JUnit 5

    Mockito

    Testcontainers


Kafka can be added later.


---

# 80. Example Dependency List

Relevant Maven dependencies:

~~~xml
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

</dependencies>
~~~

Use dependency versions managed by the Spring Boot/Spring Cloud BOM for the version family already used by the 366PI project.

---

# 81. Eureka

Interview Service registers:

    INTERVIEW-SERVICE


with:

    Eureka Server


Architecture:

    Interview Service
           |
           v
       Eureka Server


Other services can discover:

    INTERVIEW-SERVICE


without hardcoding:

    localhost:8086


---

# 82. Gateway Route

Add to API Gateway:

~~~yaml
spring:
  cloud:
    gateway:

      routes:

        - id: interview-service

          uri: lb://INTERVIEW-SERVICE

          predicates:
            - Path=/api/v1/interviews/**

        - id: admin-interview-service

          uri: lb://INTERVIEW-SERVICE

          predicates:
            - Path=/api/v1/admin/interviews/**
~~~

The Gateway uses:

    lb://INTERVIEW-SERVICE


because Eureka provides service discovery.


---

# 83. Internal Service Calls

Interview Service needs Application Service.

Possible architecture:

    Interview Service
          |
          v
    Application Service


With service discovery:

    lb://APPLICATION-SERVICE


Use a service-to-service HTTP client.

Possible technologies:

    RestClient

    WebClient

    OpenFeign


For this project, OpenFeign is convenient once Spring Cloud is already being used.


---

# 84. OpenFeign Example

Dependency:

~~~xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
~~~

Enable:

~~~java
@EnableFeignClients
@SpringBootApplication
public class InterviewServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                InterviewServiceApplication.class,
                args
        );
    }
}
~~~

Client:

~~~java
@FeignClient(
        name = "APPLICATION-SERVICE"
)
public interface ApplicationServiceFeignClient {

    @GetMapping(
        "/internal/v1/applications/{applicationId}/interview-eligibility"
    )
    ApplicationEligibility getInterviewEligibility(
            @PathVariable UUID applicationId
    );
}
~~~

This allows Eureka to resolve:

    APPLICATION-SERVICE


---

# 85. Application Service Contract

Interview Service expects Application Service to expose:

    GET
    /internal/v1/applications/{applicationId}/interview-eligibility


Response:

~~~json
{
    "applicationId": "APP-100",
    "candidateId": "CAND-100",
    "jobId": "JOB-100",
    "status": "SHORTLISTED",
    "eligibleForInterview": true
}
~~~

This contract should be treated as an internal API contract.


---

# 86. Application Status Update After Scheduling

After an interview is successfully scheduled:

    Interview Service
          |
          v
    Application Service
          |
          v
    SHORTLISTED
          |
          v
    INTERVIEW_SCHEDULED


For the MVP, this can be implemented synchronously.

Future architecture:

    Interview Service
          |
          v
    Kafka
          |
          | INTERVIEW_SCHEDULED
          v
    Application Service


---

# 87. Recommended Internal API

Application Service should eventually expose:

    PATCH
    /internal/v1/applications/{applicationId}/interview-scheduled


Request:

~~~json
{
    "interviewId": "INT-100"
}
~~~

Application Service then changes:

    SHORTLISTED

to:

    INTERVIEW_SCHEDULED


Only Interview Service should trigger this transition.


Admin should NOT directly set:

    INTERVIEW_SCHEDULED


through the normal admin application status endpoint.


---

# 88. Why Application Service Owns This Status

Even though Interview Service causes the change:

    Application Service owns application state.


Therefore:

    Interview Service:
        "Interview has been successfully booked."

Application Service:

    "My application is now in
     INTERVIEW_SCHEDULED."


This maintains ownership boundaries.


---

# 89. Double Booking Scenario

Candidate A:

    Slot 10:00


Candidate B:

    Slot 10:00


Both click:

    Book


Request A:

    Lock slot

    Slot AVAILABLE

    Book slot

    Create interview

    Commit


Request B:

    Wait for lock

    Lock acquired

    Slot BOOKED

    Throw:

    SLOT_ALREADY_BOOKED


Result:

    Candidate A -> success

    Candidate B -> 409


This is exactly the behavior we want.


---

# 90. Rescheduling Concurrency

Suppose:

    Interview A
        |
        v
    Slot 10:00


Admin chooses:

    Slot 11:00


At the same time another admin chooses the same 11:00 slot.


Both requests must lock:

    newSlot


Only one succeeds.


The second sees:

    BOOKED


and receives:

    409 CONFLICT


---

# 91. Transaction Boundaries

Scheduling must be transactional.

One transaction should include:

    Lock slot

    Validate slot

    Mark slot BOOKED

    Create interview

    Create history


If interview creation fails:

    Slot booking must rollback.


Therefore:

~~~java
@Transactional
public InterviewResponse scheduleInterview(...) {
    ...
}
~~~


This is essential.


---

# 92. Transaction Boundary for Rescheduling

Rescheduling should also be one transaction:

    BEGIN

        Lock new slot

        Validate new slot

        Lock old slot

        Release old slot

        Book new slot

        Update interview

        Save history

    COMMIT


If anything fails:

    ROLLBACK


---

# 93. Preventing Invalid Slots

Validate:

    endTime > startTime


Example:

    10:00 -> 10:30

valid.


But:

    10:30 -> 10:00

invalid.


Also reject:

    past start time


and:

    zero duration


and:

    negative duration


---

# 94. Slot Expiration

Suppose slot:

    10:00 - 10:30


Current time:

    11:00


It should not remain:

    AVAILABLE


A scheduled job can update old slots:

    AVAILABLE
        |
        v
    EXPIRED


Example future scheduled task:

~~~java
@Scheduled(fixedRate = 60000)
public void expireOldSlots() {
    // Find available slots whose end time has passed
    // and mark them EXPIRED.
}
~~~

Add this after the basic scheduling flow works.


---

# 95. Scheduled Jobs

Future scheduled tasks:

    Expire old slots

    Mark missed interviews

    Send interview reminders

    Detect upcoming interviews

    Clean temporary data


For example:

    Interview in 24 hours
          |
          v
    Notification Service
          |
          v
    Reminder email


---

# 96. Notification Flow

When interview is scheduled:

    Interview Service
          |
          v
    INTERVIEW_SCHEDULED
          |
          v
        Kafka
          |
          v
    Notification Service
          |
          +---- Candidate email
          |
          +---- Interviewer email


Candidate receives:

    Interview date

    Interview time

    Interview type

    Meeting link


Interviewer receives:

    Candidate name

    Job

    Interview time

    Interview type


---

# 97. Interview Rescheduled Event

When admin reschedules:

    Interview Service
          |
          v
    INTERVIEW_RESCHEDULED
          |
          v
        Kafka
          |
          v
    Notification Service


Candidate receives:

    "Your interview has been rescheduled."


---

# 98. Interview Cancelled Event

When cancelled:

    Interview Service
          |
          v
    INTERVIEW_CANCELLED
          |
          v
        Kafka
          |
          v
    Notification Service


Notification:

    "Your interview has been cancelled."


---

# 99. Transactional Outbox

For reliable Kafka publishing:

    Interview Service
          |
          +-- interviews table
          |
          +-- outbox table


Transaction:

    BEGIN

        Update Interview

        Insert Outbox Event

    COMMIT


Then:

    Outbox Publisher
          |
          v
        Kafka


This prevents:

    Database updated

but:

    Kafka event lost


---

# 100. Suggested Outbox Events

Events:

    INTERVIEW_SCHEDULED

    INTERVIEW_CONFIRMED

    INTERVIEW_RESCHEDULED

    INTERVIEW_CANCELLED

    INTERVIEW_STARTED

    INTERVIEW_COMPLETED

    INTERVIEW_NO_SHOW


---

# 101. Example INTERVIEW_SCHEDULED Event

~~~json
{
    "eventId": "EVENT-100",
    "eventType": "INTERVIEW_SCHEDULED",
    "interviewId": "INT-100",
    "applicationId": "APP-100",
    "candidateId": "CAND-100",
    "jobId": "JOB-100",
    "interviewerId": "INTV-100",
    "scheduledStartTime": "2026-09-15T10:00:00",
    "scheduledEndTime": "2026-09-15T10:30:00",
    "occurredAt": "2026-09-08T10:00:00"
}
~~~

---

# 102. Redis

Do not use Redis for slot ownership.

PostgreSQL is the source of truth.

For booking:

    PostgreSQL row lock


not:

    Redis lock


Redis can later be used for:

    Caching

    Rate limiting

    Temporary data


But the interview booking transaction must be protected by the database.


---

# 103. Calendar Integration

Future feature:

    Google Calendar

    Microsoft Outlook

    Microsoft Graph


Flow:

    Interview scheduled
          |
          v
    Calendar Service
          |
          v
    Calendar event created


Candidate and interviewer receive calendar invitations.


Do not add this before the core scheduling system is stable.


---

# 104. Interview Meeting Provider

Future architecture:

    Interview Service
          |
          v
    Meeting Provider
       /         \
      v           v
   Zoom       Google Meet


Interview Service stores:

    meetingProvider

    meetingLink

    meetingId


It does not need to implement the video platform itself.


---

# 105. Interview Rounds

Example:

    Application:
        APP-100


Round 1:

    Technical Interview


Round 2:

    Managerial Interview


Round 3:

    HR Interview


Each interview can have:

    applicationId

    roundNumber

    roundName


Example:

~~~text
APP-100

    INT-1
        round = 1
        Technical

    INT-2
        round = 2
        Managerial

    INT-3
        round = 3
        HR
~~~

---

# 106. Multiple Interview Rule

The MVP can allow:

    One active interview per application.


After completion:

    another round can be created.


Future implementation:

    Application
       |
       +-- Interview Round 1
       |
       +-- Interview Round 2
       |
       +-- Interview Round 3


Do not create unnecessary complexity in the first version.


---

# 107. Candidate Interview Scheduling UX

Frontend flow:

    My Applications
          |
          v
    Application
          |
          v
    Status = SHORTLISTED
          |
          v
    "Schedule Interview"
          |
          v
    Available Slots
          |
          v
    Select Date
          |
          v
    Select Time
          |
          v
    Select Interview Type
          |
          v
    Confirm
          |
          v
    Interview Scheduled


After success:

    Interview date/time displayed

    Status displayed

    Meeting link displayed when available


---

# 108. Candidate Should Not See Unavailable Slots

Backend must filter:

    AVAILABLE


and:

    future slots


The frontend should not be trusted to hide BOOKED slots.

Backend must always validate the slot again before booking.


This is important because:

    Frontend state can become stale.


---

# 109. Stale Frontend Scenario

Frontend loads:

    10:00 AVAILABLE


Candidate waits 5 minutes.

Another candidate books it.

Frontend still displays:

    AVAILABLE


First candidate clicks:

    Book


Backend checks:

    Slot status = BOOKED


Backend returns:

    409 CONFLICT


Frontend should then:

    Refresh available slots.


---

# 110. API Error Codes

Recommended:

    INTERVIEW_NOT_FOUND

    SLOT_NOT_FOUND

    SLOT_ALREADY_BOOKED

    SLOT_EXPIRED

    INTERVIEW_NOT_ALLOWED

    INTERVIEW_ALREADY_EXISTS

    INVALID_INTERVIEW_STATE

    INVALID_SLOT

    INTERVIEW_ACCESS_DENIED

    INTERVIEWER_NOT_FOUND

    INTERVIEWER_INACTIVE

    OVERLAPPING_SLOT


---

# 111. Error Response Example

~~~json
{
    "timestamp": "2026-09-15T09:59:00",
    "status": 409,
    "code": "SLOT_ALREADY_BOOKED",
    "message": "Interview slot is no longer available"
}
~~~

---

# 112. Pagination

Admin may have thousands of interviews.

Never return everything.

Use:

    Pageable


Example:

    GET /api/v1/admin/interviews?page=0&size=20


Sorting:

    ?sort=scheduledStartTime,asc


Candidate:

    GET /api/v1/interviews/me?page=0&size=10


---

# 113. Filtering

Admin should eventually filter by:

    Status

    Interviewer

    Candidate

    Job

    Date

    Interview type

    Round


Examples:

    ?status=SCHEDULED

    ?interviewerId=INTERVIEWER-100

    ?jobId=JOB-100


Later implement:

    JpaSpecificationExecutor


for flexible filtering.


---

# 114. Interview History API

Recommended:

    GET /api/v1/interviews/{interviewId}/history


Candidate can view their own history.

Admin can view any history.


Example:

~~~json
[
    {
        "action": "SCHEDULED",
        "newStartTime": "2026-09-15T10:00:00",
        "newEndTime": "2026-09-15T10:30:00",
        "newStatus": "SCHEDULED"
    },
    {
        "action": "RESCHEDULED",
        "oldStartTime": "2026-09-15T10:00:00",
        "oldEndTime": "2026-09-15T10:30:00",
        "newStartTime": "2026-09-16T15:00:00",
        "newEndTime": "2026-09-16T15:30:00"
    }
]
~~~

---

# 115. Observability

Enable:

    Spring Boot Actuator


Endpoints:

    /actuator/health

    /actuator/info

    /actuator/metrics


Important metrics:

    interview scheduling success

    interview scheduling failures

    slot booking conflicts

    interview cancellations

    interview reschedules

    database connection health

    HTTP latency


Later use:

    Prometheus

    Grafana

    OpenTelemetry


---

# 116. Spring Boot Admin

Interview Service should register with:

    Spring Boot Admin


Admin can monitor:

    Interview Service health

    JVM memory

    CPU

    HTTP requests

    Database health

    Metrics


This is operational monitoring.

It is separate from:

    Recruitment Admin Dashboard


---

# 117. Logging

Log important operations:

    Interview scheduled

    Interview rescheduled

    Interview cancelled

    Slot booking conflict

    Interview completed


Example:

~~~text
Interview scheduled
interviewId=INT-100
applicationId=APP-100
candidateId=CAND-100
slotId=SLOT-100
~~~

Do not log:

    JWT

    passwords

    secret keys

    sensitive candidate information


---

# 118. Testing Strategy

Use:

    JUnit 5

    Mockito

    MockMvc

    Testcontainers

    PostgreSQL


Test categories:

    Unit tests

    Controller tests

    Repository tests

    Integration tests

    Security tests

    Concurrency tests


---

# 119. Unit Tests

Test:

    createSlot()

    invalid slot time

    overlapping slot

    scheduleInterview()

    application not shortlisted

    candidate mismatch

    slot already booked

    expired slot

    interview already exists

    rescheduleInterview()

    invalid reschedule

    updateStatus()

    invalid status transition

    cancelInterview()


---

# 120. Most Important Unit Test

Test:

    scheduleInterview_whenSlotAlreadyBooked_shouldFail


Expected:

    SlotAlreadyBookedException


HTTP:

    409 CONFLICT


---

# 121. Concurrency Test

This is extremely important.

Create:

    Thread A

    Thread B


Both try:

    POST /api/v1/interviews


with the same:

    slotId


Expected:

    One succeeds.

    One fails.


Result:

    Request A -> 201 CREATED

    Request B -> 409 CONFLICT


This proves your scheduling system is safe under concurrency.


---

# 122. Integration Test

Use:

    Testcontainers PostgreSQL


Test:

    Create slot

    Schedule interview

    Check slot becomes BOOKED

    Check interview exists

    Check history exists


Example:

~~~text
Before:

SLOT-100
AVAILABLE


Schedule


After:

SLOT-100
BOOKED


Interview:

INT-100
SCHEDULED


History:

SCHEDULED
~~~

---

# 123. Rollback Test

Test:

    Slot booking succeeds

    Interview creation fails


Expected:

    Slot remains AVAILABLE


This proves:

    @Transactional


is working correctly.


---

# 124. Security Tests

Test:

    Candidate A accesses Candidate B interview


Expected:

    403


Test:

    Candidate calls admin endpoint


Expected:

    403


Test:

    Admin calls admin endpoint


Expected:

    success


Test:

    Unauthenticated user calls protected endpoint


Expected:

    401


---

# 125. End-to-End Test

Complete flow:

    1. Candidate registers

    2. Candidate logs in

    3. Admin creates job

    4. Admin publishes job

    5. Candidate applies

    6. Admin reviews application

    7. Admin shortlists candidate

    8. Admin creates interview slot

    9. Candidate sees slot

    10. Candidate books slot

    11. Slot becomes BOOKED

    12. Interview becomes SCHEDULED

    13. Application becomes INTERVIEW_SCHEDULED

    14. Candidate receives notification

    15. Interviewer receives notification

    16. Interview is conducted

    17. Interview becomes COMPLETED

    18. Admin marks candidate HIRED or REJECTED


This is the complete recruitment pipeline.


---

# 126. Important Architecture Rule

Do not implement this:

    Interview Service
          |
          v
    application_db


Do this:

    Interview Service
          |
          | REST / Kafka
          v
    Application Service
          |
          v
    application_db


This keeps service boundaries clean.


---

# 127. Important Application Status Rule

Do not let Admin Interview Service directly change:

    application_db


Instead:

    Interview Service
          |
          v
    Application Service
          |
          v
    INTERVIEW_SCHEDULED


Application Service remains the owner of application status.


---

# 128. Notification Architecture

Do not send email directly from the core interview transaction if using Kafka.

Avoid:

    DB transaction
        |
        +-- Save interview
        |
        +-- Send email
        |
        +-- Email fails
        |
        +-- Transaction becomes complicated


Prefer:

    Save interview
         |
         v
    Save outbox event
         |
         v
    Commit
         |
         v
    Kafka
         |
         v
    Notification Service
         |
         v
    Email


---

# 129. Future Reminder System

Possible reminders:

    24 hours before

    1 hour before

    15 minutes before


Example:

    Interview:
        10:00 AM


At:

    9:00 AM


Notification Service sends:

    "Your interview starts in 1 hour."


Interview Service can publish:

    INTERVIEW_REMINDER_DUE


or a dedicated scheduler can handle reminders.


---

# 130. Calendar Reminder

Future:

    Interview Service
          |
          v
    Calendar Integration
          |
          +-- Candidate calendar
          |
          +-- Interviewer calendar


Calendar event:

    Technical Interview

    Candidate: Sanchit

    Interviewer: Rahul

    10:00 - 10:30

    Meeting link


---

# 131. Meeting Link Generation

Future flow:

    Interview created
          |
          v
    Meeting Provider
          |
          v
    Meeting ID
          |
          v
    Meeting Link
          |
          v
    Interview updated


Example:

    meetingProvider = GOOGLE_MEET

    meetingId = ABC123

    meetingLink = https://...


---

# 132. Interview Feedback

A future table:

    interview_feedback


Fields:

    id

    interview_id

    interviewer_id

    technical_score

    communication_score

    problem_solving_score

    overall_score

    recommendation

    comments

    created_at

    updated_at


Recommendation:

    STRONG_HIRE

    HIRE

    NO_HIRE

    STRONG_NO_HIRE


Do not add this to the basic scheduling implementation unless required.


---

# 133. Why Feedback Should Be Separate

Interview scheduling and interview evaluation are different responsibilities.

Interview:

    When is the interview?


Feedback:

    How did the candidate perform?


Keeping feedback separate allows:

    multiple interviewers

    multiple rounds

    independent evaluations

    structured scoring


---

# 134. Interviewer Assignment

Current design:

    Slot belongs to interviewer


Therefore when candidate books:

    slot


the interviewer comes from:

    slot.interviewerId


This prevents candidate from choosing an arbitrary interviewer.


Flow:

    Candidate selects SLOT-100
            |
            v
    SLOT-100
            |
            +-- interviewerId = INTV-100
            |
            v
    Interview
            |
            +-- interviewerId = INTV-100


---

# 135. Why Candidate Should Not Send Interviewer ID

Wrong:

~~~json
{
    "applicationId": "APP-100",
    "slotId": "SLOT-100",
    "interviewerId": "INTERVIEWER-999"
}
~~~

Candidate should never control interviewer assignment.


Correct:

~~~json
{
    "applicationId": "APP-100",
    "slotId": "SLOT-100",
    "roundNumber": 1,
    "roundName": "Technical Interview",
    "interviewType": "VIDEO"
}
~~~


Backend gets:

    interviewerId

from:

    slot


---

# 136. Admin Slot Creation Security

Only admin should create slots.

Endpoint:

    POST /api/v1/admin/interviews/slots


Admin must be authenticated.

Admin ID comes from JWT.

The frontend should not be able to impersonate another admin.


---

# 137. Interviewer Activity

Before creating a slot, validate:

    interviewer exists

    interviewer.active = true


If:

    active = false


reject:

    INTERVIEWER_INACTIVE


This prevents scheduling interviews with inactive interviewers.


---

# 138. Interviewer Repository Validation

Before creating slot:

~~~java
Interviewer interviewer =
        interviewerRepository
                .findById(
                        request.interviewerId()
                )
                .orElseThrow(() ->
                        new InterviewerNotFoundException(
                                "Interviewer not found"
                        )
                );

if (!interviewer.isActive()) {

    throw new InterviewerInactiveException(
            "Interviewer is inactive"
    );
}
~~~


Add these exceptions if interviewer management is implemented.


---

# 139. Slot Filtering

Candidate should receive:

    AVAILABLE

and:

    startTime > current time


Do not return:

    BOOKED

    BLOCKED

    EXPIRED


---

# 140. Slot Blocking

Admin may need to block a slot.

Example:

    Interviewer becomes unavailable.


Admin can:

    AVAILABLE -> BLOCKED


This prevents candidates from booking it.


Future API:

    POST
    /api/v1/admin/interviews/slots/{slotId}/block


---

# 141. Slot Unblocking

Future:

    BLOCKED -> AVAILABLE


only if:

    startTime is still in future.


---

# 142. Slot State Machine

Recommended:

    AVAILABLE
        |
        +----> BOOKED
        |
        +----> BLOCKED
        |
        +----> EXPIRED


BOOKED can become:

    AVAILABLE


only during rescheduling/cancellation if business rules allow it.


BLOCKED can become:

    AVAILABLE


EXPIRED is final.


---

# 143. Interview State Machine

Recommended:

    SCHEDULED
        |
        +----> CONFIRMED
        |
        +----> CANCELLED
        |
        +----> NO_SHOW

    CONFIRMED
        |
        +----> IN_PROGRESS
        |
        +----> CANCELLED
        |
        +----> NO_SHOW

    IN_PROGRESS
        |
        +----> COMPLETED


Final:

    COMPLETED

    CANCELLED

    NO_SHOW


---

# 144. Service-to-Service Failure

Suppose Interview Service calls Application Service.

Application Service is down.

Then:

    Candidate tries scheduling

    Interview Service cannot verify application


Do NOT schedule the interview.

Return:

    503 SERVICE_UNAVAILABLE


This is safer than assuming eligibility.


---

# 145. Retry Strategy

For temporary service failures:

    Application Service timeout

Use:

    timeout

    retry

    circuit breaker


Future technology:

    Resilience4j


Example:

    Interview Service
          |
          v
    Application Service
          |
          X
        timeout
          |
          v
    Retry
          |
          v
    Circuit Breaker


Do not retry indefinitely.


---

# 146. Idempotency

Scheduling requests can be retried by clients.

Example:

    Candidate clicks Book

Network timeout occurs.

Candidate clicks again.


The first request may have succeeded.

The second request should not create another interview.


Use:

    applicationId + active interview rule

and:

    unique slot constraint


For stronger idempotency, future API can accept:

    Idempotency-Key


Example:

    Idempotency-Key: 7a9f...


---

# 147. Idempotency Key Future Design

Request:

~~~http
POST /api/v1/interviews
Idempotency-Key: 5b5f-1234
~~~


Store:

    idempotency_key

    candidate_id

    request_hash

    response


If the same request is repeated:

    Return previous response.


This is especially useful for mobile networks and payment-like workflows.


---

# 148. Database-per-Service Final Design

    interview_db

        |
        +-- interviewers
        |
        +-- interview_slots
        |
        +-- interviews
        |
        +-- interview_history
        |
        +-- future outbox


No other service database is accessed directly.


---

# 149. Complete Project Structure

    interview-service
    |
    +-- src/main/java/com/pi/interview
    |
    |   +-- InterviewServiceApplication.java
    |
    |   +-- controller
    |   |   +-- InterviewController.java
    |   |   +-- AdminInterviewController.java
    |   |   +-- InterviewerController.java
    |   |
    |   +-- service
    |   |   +-- InterviewService.java
    |   |   +-- InterviewServiceImpl.java
    |   |
    |   +-- repository
    |   |   +-- InterviewRepository.java
    |   |   +-- InterviewSlotRepository.java
    |   |   +-- InterviewerRepository.java
    |   |   +-- InterviewHistoryRepository.java
    |   |
    |   +-- entity
    |   |   +-- Interview.java
    |   |   +-- InterviewSlot.java
    |   |   +-- Interviewer.java
    |   |   +-- InterviewHistory.java
    |   |
    |   +-- dto
    |   |   +-- CreateSlotRequest.java
    |   |   +-- SlotResponse.java
    |   |   +-- ScheduleInterviewRequest.java
    |   |   +-- InterviewResponse.java
    |   |   +-- RescheduleInterviewRequest.java
    |   |   +-- UpdateInterviewStatusRequest.java
    |   |
    |   +-- enums
    |   |   +-- InterviewStatus.java
    |   |   +-- InterviewType.java
    |   |   +-- SlotStatus.java
    |   |
    |   +-- mapper
    |   |   +-- InterviewMapper.java
    |   |
    |   +-- client
    |   |   +-- ApplicationServiceClient.java
    |   |
    |   +-- exception
    |   |   +-- InterviewNotFoundException.java
    |   |   +-- SlotNotFoundException.java
    |   |   +-- SlotAlreadyBookedException.java
    |   |   +-- SlotExpiredException.java
    |   |   +-- InterviewNotAllowedException.java
    |   |   +-- InterviewAlreadyExistsException.java
    |   |   +-- InvalidInterviewStateException.java
    |   |   +-- InvalidSlotException.java
    |   |   +-- InterviewAccessDeniedException.java
    |   |   +-- GlobalExceptionHandler.java
    |   |
    |   +-- config
    |       +-- SecurityConfig.java
    |
    +-- src/main/resources
        |
        +-- application.yml
        |
        +-- db/migration
            |
            +-- V1__create_interviewers_table.sql
            +-- V2__create_interview_slots_table.sql
            +-- V3__create_interviews_table.sql
            +-- V4__create_interview_history_table.sql


---

# 150. Recommended Implementation Order

Implement in this exact order:

    Step 1
    Create Spring Boot project


    Step 2
    Configure PostgreSQL


    Step 3
    Create interview_db


    Step 4
    Configure Flyway


    Step 5
    Create InterviewStatus


    Step 6
    Create InterviewType


    Step 7
    Create SlotStatus


    Step 8
    Create Interviewer entity


    Step 9
    Create InterviewSlot entity


    Step 10
    Create Interview entity


    Step 11
    Create InterviewHistory entity


    Step 12
    Create repositories


    Step 13
    Create DTOs


    Step 14
    Create mapper


    Step 15
    Create ApplicationServiceClient


    Step 16
    Create InterviewService


    Step 17
    Implement slot creation


    Step 18
    Implement available slots


    Step 19
    Implement interview scheduling


    Step 20
    Add pessimistic locking


    Step 21
    Add duplicate interview protection


    Step 22
    Add candidate APIs


    Step 23
    Add admin APIs


    Step 24
    Add rescheduling


    Step 25
    Add cancellation


    Step 26
    Add interview status transitions


    Step 27
    Add history


    Step 28
    Add exception handling


    Step 29
    Add JWT security


    Step 30
    Connect Application Service


    Step 31
    Register with Eureka


    Step 32
    Add Gateway route


    Step 33
    Add Actuator


    Step 34
    Add Spring Boot Admin


    Step 35
    Add unit tests


    Step 36
    Add integration tests


    Step 37
    Add concurrency tests


    Step 38
    Add Kafka


    Step 39
    Add Outbox


    Step 40
    Add Notification integration


---

# 151. Testing Checklist

    [ ] Project starts successfully

    [ ] PostgreSQL connects

    [ ] Flyway migrations execute

    [ ] Interviewer can be created

    [ ] Slot can be created

    [ ] Invalid slot time is rejected

    [ ] Past slot is rejected

    [ ] Overlapping slot is rejected

    [ ] Available slots are returned

    [ ] BOOKED slots are not returned

    [ ] EXPIRED slots are not returned

    [ ] Candidate can schedule interview

    [ ] Candidate must be shortlisted

    [ ] Candidate cannot schedule another candidate's application

    [ ] Candidate cannot book BOOKED slot

    [ ] Concurrent booking is safe

    [ ] Slot becomes BOOKED

    [ ] Interview becomes SCHEDULED

    [ ] Interview history is created

    [ ] Candidate can view own interviews

    [ ] Candidate cannot view another candidate's interview

    [ ] Admin can view all interviews

    [ ] Admin can reschedule

    [ ] Old slot becomes AVAILABLE after rescheduling

    [ ] New slot becomes BOOKED

    [ ] Reschedule history is created

    [ ] Admin can cancel

    [ ] Status transitions are validated

    [ ] Actuator works

    [ ] Eureka registration works

    [ ] Gateway route works


---

# 152. Definition of Done

Interview Service is considered complete when:

    [ ] interview-service created

    [ ] interview_db created

    [ ] PostgreSQL configured

    [ ] Flyway configured

    [ ] Interviewer table created

    [ ] Interview slots table created

    [ ] Interviews table created

    [ ] Interview history table created

    [ ] InterviewStatus created

    [ ] InterviewType created

    [ ] SlotStatus created

    [ ] Entities created

    [ ] Repositories created

    [ ] DTOs created

    [ ] Mapper created

    [ ] Application Service client created

    [ ] Candidate APIs created

    [ ] Admin APIs created

    [ ] Slot creation implemented

    [ ] Slot overlap validation implemented

    [ ] Slot expiration implemented

    [ ] Interview scheduling implemented

    [ ] Pessimistic locking implemented

    [ ] Duplicate interview prevention implemented

    [ ] Candidate ownership validation implemented

    [ ] Interview rescheduling implemented

    [ ] Interview cancellation implemented

    [ ] Interview status transition validation implemented

    [ ] Interview history implemented

    [ ] JWT security implemented

    [ ] Admin authorization implemented

    [ ] Eureka configured

    [ ] Gateway configured

    [ ] Actuator configured

    [ ] Spring Boot Admin configured

    [ ] Unit tests implemented

    [ ] Controller tests implemented

    [ ] Integration tests implemented

    [ ] Concurrency tests implemented

    [ ] Kafka events added

    [ ] Outbox added

    [ ] Notification integration added


---

# 153. Final API Summary

## Candidate

    GET
    /api/v1/interviews/slots?applicationId={applicationId}


    POST
    /api/v1/interviews


    GET
    /api/v1/interviews/me


    GET
    /api/v1/interviews/{interviewId}


## Admin

    POST
    /api/v1/admin/interviews/slots


    GET
    /api/v1/admin/interviews


    PATCH
    /api/v1/admin/interviews/{interviewId}/status


    POST
    /api/v1/admin/interviews/{interviewId}/reschedule


    POST
    /api/v1/admin/interviews/{interviewId}/cancel


## Internal

    GET
    /internal/v1/applications/{applicationId}/interview-eligibility


Application Service:

    PATCH
    /internal/v1/applications/{applicationId}/interview-scheduled


---

# 154. Complete Example

Assume:

    Candidate:
        CAND-001

    Job:
        JOB-001

    Application:
        APP-001

Application status:

    SHORTLISTED


Admin creates:

    Slot:
        SLOT-001

    Interviewer:
        INT-001

    Time:
        10:00 - 10:30


Candidate requests:

    GET /api/v1/interviews/slots?applicationId=APP-001


Interview Service calls Application Service:

    applicationId = APP-001

Response:

    candidateId = CAND-001

    jobId = JOB-001

    status = SHORTLISTED

    eligible = true


Candidate books:

    SLOT-001


Interview Service:

    Locks SLOT-001

    Checks AVAILABLE

    Changes SLOT-001 to BOOKED

    Creates INT-001

    Status = SCHEDULED

    Creates history


Then:

    Application Service

changes:

    SHORTLISTED

to:

    INTERVIEW_SCHEDULED


Then future event:

    INTERVIEW_SCHEDULED


is published to Kafka.


Notification Service sends:

    Candidate:
        Interview scheduled


    Interviewer:
        New interview assigned


---

# 155. Final 366PI Recruitment Architecture

The complete architecture is now:

                         FRONTEND
                            |
                            v
                      API GATEWAY
                            |
          +-----------------+------------------+
          |                 |                  |
          v                 v                  v
     AUTH SERVICE      USER SERVICE       JOB SERVICE
          |                 |                  |
          v                 v                  v
       auth_db           user_db             job_db
                                              
                            |
                            v
                    APPLICATION SERVICE
                            |
                            v
                       application_db
                            |
                            |
                 Candidate shortlisted
                            |
                            v
                    INTERVIEW SERVICE
                            |
                            +-------------------+
                            |                   |
                            v                   v
                     interview_db          Kafka/Outbox
                                                |
                                                v
                                      NOTIFICATION SERVICE


Interview Service owns:

    Interview
    Slots
    Interviewers
    Scheduling
    Rescheduling
    Cancellation
    Interview status
    Interview history


Application Service owns:

    Application
    Application status
    Shortlisting
    Interview eligibility


Job Service owns:

    Job
    Job lifecycle
    Job publishing


Auth Service owns:

    Login
    Password
    JWT
    Roles


User Service owns:

    Candidate profile


Notification Service owns:

    Email
    SMS
    Notifications


---

# 156. Most Important Rules

Rule 1:

Never accept candidateId from frontend.

Use JWT.


Rule 2:

Never directly access application_db.

Use Application Service.


Rule 3:

Never directly access job_db.

Use Job Service if job information is needed.


Rule 4:

Never allow candidate to choose interviewerId.

Interviewer comes from selected slot.


Rule 5:

Always lock the slot before booking.


Rule 6:

Always use a database constraint to protect against double booking.


Rule 7:

Always use a transaction when booking a slot.


Rule 8:

Always validate that application status is SHORTLISTED.


Rule 9:

Application Service owns application status.


Rule 10:

Interview Service owns interview status.


Rule 11:

Admin controls interviewer slots.


Rule 12:

Candidate can choose only from available slots.


Rule 13:

Never trust frontend slot availability.


Rule 14:

Maintain interview history.


Rule 15:

Use Flyway for schema changes.


Rule 16:

Use DTOs instead of returning entities.


Rule 17:

Use pagination.


Rule 18:

Use Actuator and Eureka from the beginning.


Rule 19:

Add Kafka after the synchronous flow works.


Rule 20:

Use Transactional Outbox for reliable events.


---

# 157. Final Mental Model

Think of the entire system like this:

    JOB
     |
     | Candidate applies
     v
    APPLICATION
     |
     | Admin reviews
     v
    SHORTLISTED
     |
     | Candidate chooses slot
     v
    INTERVIEW
     |
     | Interview happens
     v
    COMPLETED
     |
     | Admin decides
     v
    HIRED / REJECTED


The ownership is:

    Job Service
        |
        +-- JOB


    Application Service
        |
        +-- APPLICATION


    Interview Service
        |
        +-- INTERVIEW


This separation is the foundation of the 366PI recruitment platform.

The most critical technical problem in Interview Service is not simply creating an interview record.

The critical problem is:

    "How do we guarantee that two candidates
     cannot book the same interviewer slot?"

The answer is:

    PostgreSQL transaction
            +
    Pessimistic row locking
            +
    Slot status validation
            +
    Unique database constraint


That combination gives the Interview Service a reliable foundation for production-level interview scheduling.