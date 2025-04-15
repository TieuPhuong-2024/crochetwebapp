package org.crochet.service;

public interface ViewCountService {
    void incrementViewCount(String patternId, String ipAddress);
}
