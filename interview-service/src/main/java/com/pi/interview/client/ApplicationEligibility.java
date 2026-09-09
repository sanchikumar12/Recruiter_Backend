package com.pi.interview.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pi.interview.enums.ApplicationStatus;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationEligibility(
        UUID applicationId,
        UUID candidateId,
        UUID jobId,
        @JsonAlias({"status", "currentStatus"})
        ApplicationStatus status,
        @JsonAlias({"eligibleForInterview", "isEligible", "eligible"})
        boolean eligibleForInterview
) {
}
