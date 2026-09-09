package com.pi.interview.client;

import com.pi.interview.exception.InterviewNotAllowedException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceClientImpl implements ApplicationServiceClient {

    private final ApplicationServiceFeignClient feignClient;

    @Override
    public ApplicationEligibility getInterviewEligibility(UUID applicationId) {
        try {
            return feignClient.getInterviewEligibility(applicationId);
        } catch (FeignException.NotFound ex) {
            log.warn("Application {} not found in Application Service", applicationId);
            throw new InterviewNotAllowedException("Application not found");
        } catch (FeignException ex) {
            log.error("Error communicating with Application Service for application {}: {}", applicationId, ex.getMessage());
            throw new InterviewNotAllowedException("Unable to verify interview eligibility: " + ex.getMessage());
        }
    }

    @Override
    public void markInterviewScheduled(UUID applicationId, UUID systemId) {
        try {
            String systemHeader = systemId != null ? systemId.toString() : "00000000-0000-0000-0000-000000000000";
            feignClient.markInterviewScheduled(applicationId, systemHeader);
        } catch (FeignException ex) {
            log.error("Failed to notify Application Service that interview was scheduled for {}: {}", applicationId, ex.getMessage());
            // Log warning but let interview proceed if desired, or throw exception
        }
    }
}
