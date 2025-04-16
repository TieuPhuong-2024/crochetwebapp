package org.crochet.service;

public interface GoogleAnalyticsService {
    void trackPatternView(String patternId, String ipAddress, String userAgent);
} 