package com.pi.application.mapper;

import com.pi.application.dto.ApplicationResponse;
import com.pi.application.dto.ApplicationStatusHistoryResponse;
import com.pi.application.entity.ApplicationStatusHistory;
import com.pi.application.entity.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(JobApplication application) {
        if (application == null) {
            return null;
        }

        return new ApplicationResponse(
                application.getId(),
                application.getJobId(),
                application.getCandidateId(),
                application.getStatus(),
                application.getResumeUrl(),
                application.getCoverNote(),
                application.getRejectionReason(),
                application.getAppliedAt(),
                application.getUpdatedAt()
        );
    }

    public ApplicationStatusHistoryResponse toHistoryResponse(ApplicationStatusHistory history) {
        if (history == null) {
            return null;
        }

        return new ApplicationStatusHistoryResponse(
                history.getId(),
                history.getApplicationId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getComments(),
                history.getChangedAt()
        );
    }
}
