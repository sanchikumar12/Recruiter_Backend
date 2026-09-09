package com.pi.user.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    private final RestClient restClient;

    public AuthServiceClient(@Value("${services.auth-service.url:http://localhost:8083}") String authServiceUrl) {
        log.info("Configuring AuthServiceClient with base URL: {}", authServiceUrl);
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    public AuthRegisterResponse register(String email, String password, String role) {
        try {
            AuthRegisterRequest request = new AuthRegisterRequest(email, password, role);
            return restClient.post()
                    .uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AuthRegisterResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("Auth-service call failed with status {} body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalArgumentException("Authentication service registration failed: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Failed to connect to auth-service: {}", ex.getMessage());
            throw new IllegalStateException("Unable to communicate with auth-service: " + ex.getMessage(), ex);
        }
    }
}
