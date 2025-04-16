package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.config.GoogleAnalyticsConfig;
import org.crochet.service.GoogleAnalyticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAnalyticsServiceImpl implements GoogleAnalyticsService {

    private final RestTemplate analyticsRestTemplate;
    private final GoogleAnalyticsConfig googleAnalyticsConfig;
    
    @Value("${google.analytics.api-secret:}")
    private String apiSecret;
    
    private static final String GA4_COLLECT_ENDPOINT = "https://www.google-analytics.com/mp/collect";

    @Override
    public void trackPatternView(String patternId, String ipAddress, String userAgent) {
        if (!googleAnalyticsConfig.isEnabled() || googleAnalyticsConfig.getMeasurementId().isEmpty()) {
            log.debug("Google Analytics is disabled or measurement ID is not set");
            return;
        }
        
        try {
            String clientId = generateClientId(ipAddress);
            String url = GA4_COLLECT_ENDPOINT + "?measurement_id=" + googleAnalyticsConfig.getMeasurementId() 
                + "&api_secret=" + apiSecret;
            
            Map<String, Object> event = new HashMap<>();
            event.put("name", "pattern_view");
            
            Map<String, Object> params = new HashMap<>();
            params.put("pattern_id", patternId);
            event.put("params", params);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("client_id", clientId);
            payload.put("user_agent", userAgent);
            payload.put("events", new Object[]{event});
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            analyticsRestTemplate.postForObject(url, request, String.class);
            
            log.debug("Tracked pattern view event for pattern ID: {}", patternId);
        } catch (Exception e) {
            log.error("Error sending data to Google Analytics: {}", e.getMessage());
        }
    }
    
    private String generateClientId(String ipAddress) {
        // Tạo một client ID dựa trên địa chỉ IP để theo dõi người dùng nhưng vẫn giữ tính ẩn danh
        return UUID.nameUUIDFromBytes(ipAddress.getBytes()).toString();
    }
} 