package com.exercise.publicapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // Without timeouts, a hanging downstream service would hang every public API request too.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     @Value("${services.connect-timeout}") Duration connectTimeout,
                                     @Value("${services.read-timeout}") Duration readTimeout) {
        return builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }
}