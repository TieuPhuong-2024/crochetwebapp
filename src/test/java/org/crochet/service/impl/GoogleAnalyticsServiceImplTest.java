package org.crochet.service.impl;

import org.crochet.config.GoogleAnalyticsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GoogleAnalyticsServiceImplTest {

    @Mock
    private RestTemplate analyticsRestTemplate;

    @Mock
    private GoogleAnalyticsConfig googleAnalyticsConfig;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    private GoogleAnalyticsServiceImpl analyticsService;

    private static final String TEST_PATTERN_ID = "pattern-123";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";
    private static final String TEST_MEASUREMENT_ID = "G-TESTID123";
    private static final String TEST_API_SECRET = "test_secret";

    @BeforeEach
    void setUp() {
        analyticsService = new GoogleAnalyticsServiceImpl(analyticsRestTemplate, googleAnalyticsConfig);
        // Sử dụng reflection để gán API secret
        try {
            java.lang.reflect.Field apiSecretField = GoogleAnalyticsServiceImpl.class.getDeclaredField("apiSecret");
            apiSecretField.setAccessible(true);
            apiSecretField.set(analyticsService, TEST_API_SECRET);
        } catch (Exception e) {
            // Handle exception
        }
    }

    @Test
    void testTrackPatternViewWhenAnalyticsEnabled() {
        // Arrange
        when(googleAnalyticsConfig.isEnabled()).thenReturn(true);
        when(googleAnalyticsConfig.getMeasurementId()).thenReturn(TEST_MEASUREMENT_ID);

        // Act
        analyticsService.trackPatternView(TEST_PATTERN_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

        // Assert
        verify(analyticsRestTemplate, times(1)).postForObject(
                urlCaptor.capture(),
                requestCaptor.capture(),
                eq(String.class));

        String capturedUrl = urlCaptor.getValue();
        HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();

        // Kiểm tra URL
        assertNotNull(capturedUrl);
        assertEquals(
                "https://www.google-analytics.com/mp/collect?measurement_id=" + TEST_MEASUREMENT_ID + "&api_secret="
                        + TEST_API_SECRET,
                capturedUrl);

        // Kiểm tra payload
        assertNotNull(capturedRequest);
        Map<String, Object> payload = capturedRequest.getBody();
        assertNotNull(payload);

        // Kiểm tra client ID
        assertNotNull(payload.get("client_id"));

        // Kiểm tra user agent
        assertEquals(TEST_USER_AGENT, payload.get("user_agent"));

        // Kiểm tra events
        Object[] events = (Object[]) payload.get("events");
        assertNotNull(events);
        assertEquals(1, events.length);

        Map<String, Object> event = (Map<String, Object>) events[0];
        assertEquals("pattern_view", event.get("name"));

        Map<String, Object> params = (Map<String, Object>) event.get("params");
        assertNotNull(params);
        assertEquals(TEST_PATTERN_ID, params.get("pattern_id"));
    }

    @Test
    void testTrackPatternViewWhenAnalyticsDisabled() {
        // Arrange
        when(googleAnalyticsConfig.isEnabled()).thenReturn(false);

        // Act
        analyticsService.trackPatternView(TEST_PATTERN_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

        // Assert
        verify(analyticsRestTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void testTrackPatternViewWhenMeasurementIdEmpty() {
        // Arrange
        when(googleAnalyticsConfig.isEnabled()).thenReturn(true);
        when(googleAnalyticsConfig.getMeasurementId()).thenReturn("");

        // Act
        analyticsService.trackPatternView(TEST_PATTERN_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

        // Assert
        verify(analyticsRestTemplate, never()).postForObject(anyString(), any(), any());
    }
}