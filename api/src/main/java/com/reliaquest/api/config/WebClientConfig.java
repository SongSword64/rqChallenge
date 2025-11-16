package com.reliaquest.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient employeeClient(WebClient.Builder builder) {
        return builder.baseUrl("http://localhost:8112/api/v1/employee").build();
    }
}
