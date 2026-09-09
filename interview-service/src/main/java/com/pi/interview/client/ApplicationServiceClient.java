package com.pi.interview.client;

import java.util.UUID;

public interface ApplicationServiceClient {

    ApplicationEligibility getInterviewEligibility(UUID applicationId);

    void markInterviewScheduled(UUID applicationId, UUID systemId);
}
