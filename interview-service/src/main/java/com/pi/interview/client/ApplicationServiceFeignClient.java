package com.pi.interview.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "APPLICATION-SERVICE",
        url = "${services.application-service.url:http://localhost:8085}"
)
public interface ApplicationServiceFeignClient {

    @GetMapping("/api/v1/internal/applications/{applicationId}/eligibility")
    ApplicationEligibility getInterviewEligibility(@PathVariable("applicationId") UUID applicationId);

    @PostMapping("/api/v1/internal/applications/{applicationId}/interview-scheduled")
    void markInterviewScheduled(
            @PathVariable("applicationId") UUID applicationId,
            @RequestHeader(value = "X-System-Id", required = false) String systemId
    );
}
