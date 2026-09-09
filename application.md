# Application Service - Implementation Guide

## 1. Overview

The Application Service is one of the core microservices of the 366PI recruitment platform.

Its main responsibility is to manage the complete lifecycle of a candidate's application for a job.

The service sits between the Job Service and the Interview Service.

The basic recruitment flow is:

Candidate:

    Register
       |
       v
    Login
       |
       v
    View Jobs
       |
       v
    Apply for Job
       |
       v
    Application Created
       |
       v
    Under Review
       |
       v
    Shortlisted
       |
       v
    Schedule Interview
       |
       v
    Interview Scheduled
       |
       v
    Hired / Rejected


The Application Service owns the application part of this flow.

It does NOT own:

- User authentication
- Passwords
- Candidate profile
- Job creation
- Job publishing
- Interview slots
- Interview scheduling
- Email delivery

Those responsibilities belong to other services.


---

# 2. Service Information

Service Name:

    application-service

Package:

    com.pi.application

Port:

    8085

Database:

    application_db

Database Type:

    PostgreSQL


Example architecture:

    Frontend
       |
       v
    API Gateway
       |
       +-------------------+
       |                   |
       v                   v
    Job Service      Application Service
                           |
                           v
                    Interview Service


Infrastructure:

    Eureka Server
        |
        +-- Auth Service
        +-- User Service
        +-- Job Service
        +-- Application Service
        +-- Interview Service
        +-- Notification Service


---

# 3. Responsibility of Application Service

Application Service is responsible for:

1. Creating applications
2. Preventing duplicate applications
3. Checking whether a job is open for applications
4. Storing candidate applications
5. Allowing candidates to view their applications
6. Allowing candidates to withdraw applications
7. Allowing admins to view applications
8. Allowing admins to review applications
9. Shortlisting candidates
10. Rejecting candidates
11. Maintaining application status
12. Maintaining application status history
13. Providing interview eligibility information
14. Communicating with Job Service
15. Communicating with Interview Service
16. Publishing application events in the future
17. Maintaining application audit information


---

# 4. What Application Service Does Not Own

Application Service should NOT manage:

Authentication:

    Auth Service

Candidate Profile:

    User Service

Job:

    Job Service

Interview:

    Interview Service

Email:

    Notification Service


For example, Application Service should not contain:

    password
    username
    interviewSlot
    interviewer
    meetingLink


Instead, it stores references:

    candidateId
    jobId


This keeps microservice boundaries clean.


---

# 5. Microservice Architecture

The complete architecture is:

    +----------------------+
    |      Frontend        |
    +----------+-----------+
               |
               v
    +----------------------+
    |     API Gateway      |
    +----------+-----------+
               |
       +-------+--------+
       |                |
       v                v
+-------------+   +-------------------+
| Job Service |   | Application       |
|             |   | Service           |
+------+------+   +---------+---------+
       |                    |
       v                    v
   job_db             application_db
                            |
                            |
                            v
                    +---------------+
                    | Interview     |
                    | Service       |
                    +-------+-------+
                            |
                            v
                     interview_db


Infrastructure:

    +----------------+
    | Eureka Server  |
    +----------------+

    +----------------+
    | Spring Boot    |
    | Admin          |
    +----------------+

    +----------------+
    | Kafka          |
    +----------------+


---

# 6. Database-per-Service Rule

Each service owns its own database.

Example:

    Auth Service
        |
        +-- auth_db

    User Service
        |
        +-- user_db

    Job Service
        |
        +-- job_db

    Application Service
        |
        +-- application_db

    Interview Service
        |
        +-- interview_db


Application Service must NEVER directly access:

    auth_db
    user_db
    job_db
    interview_db


Wrong:

    Application Service
           |
           v
        job_db


Correct:

    Application Service
           |
           | REST API
           v
       Job Service
           |
           v
         job_db


This is one of the most important microservice rules.


---

# 7. Technology Stack

Use:

    Java 17
    Spring Boot
    Spring Web
    Spring Data JPA
    Spring Security
    PostgreSQL
    Flyway
    Bean Validation
    JWT
    Eureka Client
    Spring Boot Actuator
    Springdoc OpenAPI
    Lombok
    JUnit 5
    Mockito
    Testcontainers


Future technologies:

    Kafka
    Redis
    Prometheus
    Grafana
    OpenTelemetry
    OpenSearch


Do not introduce all of these on day one.

First make the Application Service stable with:

    Spring Boot
    PostgreSQL
    JPA
    Flyway
    REST
    Eureka
    Security


---

# 8. Project Structure

Recommended structure:

    application-service
    |
    +-- src/main/java/com/pi/application
    |
    |   +-- ApplicationServiceApplication.java
    |
    |   +-- controller
    |   |   +-- ApplicationController.java
    |   |   +-- AdminApplicationController.java
    |   |   +-- InternalApplicationController.java
    |   |
    |   +-- service
    |   |   +-- ApplicationService.java
    |   |   +-- ApplicationServiceImpl.java
    |   |
    |   +-- repository
    |   |   +-- ApplicationRepository.java
    |   |   +-- ApplicationStatusHistoryRepository.java
    |   |
    |   +-- entity
    |   |   +-- JobApplication.java
    |   |   +-- ApplicationStatusHistory.java
    |   |
    |   +-- dto
    |   |   +-- CreateApplicationRequest.java
    |   |   +-- ApplicationResponse.java
    |   |   +-- UpdateApplicationStatusRequest.java
    |   |   +-- InterviewEligibilityResponse.java
    |   |
    |   +-- enums
    |   |   +-- ApplicationStatus.java
    |   |
    |   +-- mapper
    |   |   +-- ApplicationMapper.java
    |   |
    |   +-- client
    |   |   +-- JobServiceClient.java
    |   |
    |   +-- exception
    |   |   +-- ApplicationNotFoundException.java
    |   |   +-- DuplicateApplicationException.java
    |   |   +-- InvalidApplicationStateException.java
    |   |   +-- GlobalExceptionHandler.java
    |   |
    |   +-- config
    |       +-- SecurityConfig.java
    |
    +-- src/main/resources
        |
        +-- application.yml
        |
        +-- db
            |
            +-- migration
                +-- V1__create_applications_table.sql
                +-- V2__create_application_status_history.sql


---

# 9. Application Lifecycle

An application starts with:

    APPLIED


Then the normal flow is:

    APPLIED
       |
       v
    UNDER_REVIEW
       |
       v
    SHORTLISTED
       |
       v
    INTERVIEW_SCHEDULED
       |
       v
    HIRED


An application can also be rejected:

    APPLIED
       |
       v
    REJECTED


or:

    UNDER_REVIEW
       |
       v
    REJECTED


or:

    SHORTLISTED
       |
       v
    REJECTED


A candidate can withdraw before being shortlisted:

    APPLIED
       |
       v
    WITHDRAWN


or:

    UNDER_REVIEW
       |
       v
    WITHDRAWN


---

# 10. ApplicationStatus Enum

Create:

    src/main/java/com/pi/application/enums/ApplicationStatus.java


Code:

```java
package com.pi.application.enums;

public enum ApplicationStatus {

    APPLIED,

    UNDER_REVIEW,

    SHORTLISTED,

    INTERVIEW_SCHEDULED,

    HIRED,

    REJECTED,

    WITHDRAWN
}
```
