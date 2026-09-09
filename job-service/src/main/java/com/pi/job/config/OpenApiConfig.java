package com.pi.job.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("366PI Job Management Service API")
                        .description("Microservice responsible for job requisition lifecycle, posting, publishing, filtering, and candidate job discovery.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("366PI Engineering Team")
                                .email("engineering@366pi.com")));
    }
}
