package com.medicalstorage.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Application-level Spring beans.
 */
@Configuration
public class AppConfig {

    /**
     * Shared RestTemplate used by SeaweedFSClient to communicate with the
     * SeaweedFS master and volume nodes.
     *
     * Timeouts are deliberately generous because large DICOM files may take
     * a few seconds to transfer.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }
}
