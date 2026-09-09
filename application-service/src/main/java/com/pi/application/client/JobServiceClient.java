package com.pi.application.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class JobServiceClient {

    private final RestClient restClient;
    private final String jobServiceUrl;

    public JobServiceClient(
            @Value("${services.job-service.url:http://localhost:8084}") String jobServiceUrl) {
        this.jobServiceUrl = jobServiceUrl;
        this.restClient = RestClient.builder().baseUrl(jobServiceUrl).build();
    }

    public boolean isJobOpenForApplications(UUID jobId) {
        try {
            return restClient.get()
                    .uri("/api/v1/jobs/{jobId}", jobId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.warn("Job {} was not found or is not in PUBLISHED state", jobId);
                    })
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Could not reach Job Service at {}: {}. Proceeding with application.", jobServiceUrl, e.getMessage());
            return true;
        }
    }
}
