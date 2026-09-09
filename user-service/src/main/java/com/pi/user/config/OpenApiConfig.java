package com.pi.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("366PI Recruitment Platform - User Service API")
                        .description("Production-grade Candidate Profile Domain Service. Provides endpoints for registering candidate profiles, querying profiles by UUID, and validating profile lifecycle state.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("366PI Engineering Team")
                                .email("engineering@366pi.com")
                                .url("https://366pi.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server")
                ));
    }
}
