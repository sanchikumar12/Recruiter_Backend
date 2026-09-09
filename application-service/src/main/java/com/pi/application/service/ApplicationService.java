package com.pi.application.service;

import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.ApplicationStatsResponse;
import com.pi.application.dto.ApplicationStatusHistoryResponse;
import com.pi.application.dto.CreateApplicationRequest;
import com.pi.application.dto.InterviewEligibilityResponse;
import com.pi.application.dto.UpdateApplicationStatusRequest;
import com.pi.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse applyForJob(CreateApplicationRequest request, UUID candidateId);

    ApplicationResponse getApplicationById(UUID id, UUID requestingUser, boolean isAdmin);

    Page<ApplicationResponse> getCandidateApplications(UUID candidateId, Pageable pageable);

    ApplicationResponse withdrawApplication(UUID applicationId, UUID candidateId);

    Page<ApplicationResponse> getAdminApplications(UUID jobId, ApplicationStatus status, Pageable pageable);

    ApplicationResponse updateApplicationStatus(UUID applicationId, UpdateApplicationStatusRequest request, UUID adminId);

    InterviewEligibilityResponse checkInterviewEligibility(UUID applicationId);

    void markInterviewScheduled(UUID applicationId, UUID changedBy);

    List<ApplicationStatusHistoryResponse> getApplicationHistory(UUID applicationId);

    ApplicationStatsResponse getStats();
}
