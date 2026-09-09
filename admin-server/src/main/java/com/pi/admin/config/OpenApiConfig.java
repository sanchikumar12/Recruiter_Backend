package com.pi.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adminServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("366PI Spring Boot Admin Server API")
                        .description("Monitoring and management infrastructure server for the 366PI Recruitment Platform microservices cluster.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("366PI DevOps & Infrastructure Team")
                                .email("devops@366pi.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Admin Server")
                ));
    }
}
