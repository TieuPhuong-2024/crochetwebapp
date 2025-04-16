package org.crochet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GoogleAnalyticsConfig {
    
    @Value("${google.analytics.measurement-id:}")
    private String measurementId;
    
    @Value("${google.analytics.enabled:false}")
    private boolean enabled;
    
    @Bean
    public RestTemplate analyticsRestTemplate() {
        return new RestTemplate();
    }
    
    public String getMeasurementId() {
        return measurementId;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
} 