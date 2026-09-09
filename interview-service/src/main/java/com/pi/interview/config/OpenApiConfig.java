package com.pi.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI interviewServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("366PI Interview Scheduling & Management Service API")
                        .description("RESTful microservice managing interviewer availability, slot bookings, candidate scheduling, and interview lifecycle")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("366PI Platform Engineering")
                                .email("engineering@366pi.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
