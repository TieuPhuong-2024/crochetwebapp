package org.crochet.service;

public interface ViewCountService {
    void incrementViewCount(String patternId, String ipAddress);
    void incrementViewCount(String patternId, String ipAddress, String userAgent);
}
