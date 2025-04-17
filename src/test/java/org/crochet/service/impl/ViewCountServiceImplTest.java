package org.crochet.service.impl;

import org.crochet.model.FreePattern;
import org.crochet.service.FreePatternService;
import org.crochet.service.GoogleAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ViewCountServiceImplTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private FreePatternService freePatternService;
    @Mock
    private GoogleAnalyticsService googleAnalyticsService;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ViewCountServiceImpl viewCountService;

    private static final String PATTERN_ID = "pattern-1";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String USER_AGENT = "JUnit-Agent";
    private static final String REDIS_KEY = "pattern:" + PATTERN_ID + ":ip:" + IP_ADDRESS;

    @BeforeEach
    void setUp() {
        viewCountService = new ViewCountServiceImpl(redisTemplate, freePatternService, googleAnalyticsService);
    }

    @Test
    void testIncrementViewCount_FirstTime_ShouldIncreaseAndTrack() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(REDIS_KEY)).thenReturn(false);
        FreePattern pattern = new FreePattern();
        pattern.setId(PATTERN_ID);
        pattern.setViewCount(5L);
        when(freePatternService.findById(PATTERN_ID)).thenReturn(pattern);
        when(freePatternService.save(any(FreePattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        viewCountService.incrementViewCount(PATTERN_ID, IP_ADDRESS, USER_AGENT);

        // Assert
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set(REDIS_KEY, "1", 24, TimeUnit.HOURS);
        verify(freePatternService, times(1)).findById(PATTERN_ID);
        verify(freePatternService, times(1)).save(any(FreePattern.class));
        verify(googleAnalyticsService, times(1)).trackPatternView(PATTERN_ID, IP_ADDRESS, USER_AGENT);
    }

    @Test
    void testIncrementViewCount_AlreadyExists_ShouldNotIncreaseButStillTrack() {
        // Arrange
        when(redisTemplate.hasKey(REDIS_KEY)).thenReturn(true);

        // Act
        viewCountService.incrementViewCount(PATTERN_ID, IP_ADDRESS, USER_AGENT);

        // Assert
        verify(redisTemplate, never()).opsForValue();
        verify(freePatternService, never()).findById(anyString());
        verify(freePatternService, never()).save(any(FreePattern.class));
        verify(googleAnalyticsService, times(1)).trackPatternView(PATTERN_ID, IP_ADDRESS, USER_AGENT);
    }

    @Test
    void testIncrementViewCount_WithoutUserAgent() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(REDIS_KEY)).thenReturn(false);
        FreePattern pattern = new FreePattern();
        pattern.setId(PATTERN_ID);
        pattern.setViewCount(0L);
        when(freePatternService.findById(PATTERN_ID)).thenReturn(pattern);
        when(freePatternService.save(any(FreePattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        viewCountService.incrementViewCount(PATTERN_ID, IP_ADDRESS);

        // Assert
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set(REDIS_KEY, "1", 24, TimeUnit.HOURS);
        verify(freePatternService, times(1)).findById(PATTERN_ID);
        verify(freePatternService, times(1)).save(any(FreePattern.class));
        verify(googleAnalyticsService, times(1)).trackPatternView(PATTERN_ID, IP_ADDRESS, null);
    }
}