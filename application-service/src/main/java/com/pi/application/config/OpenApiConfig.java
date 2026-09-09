package com.pi.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI applicationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("366PI Application Management Service API")
                        .description("Microservice managing candidate job applications, recruitment review lifecycle, status history auditing, and interview eligibility.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("366PI Engineering Team")
                                .email("engineering@366pi.com")));
    }
}
