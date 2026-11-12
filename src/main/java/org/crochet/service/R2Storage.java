package org.crochet.service;

import java.time.Duration;

public interface R2Storage {
    record Metadata(long size, String contentType) { }

    String presign(String bucket, String key, String contentType, long size, Duration validity);
    Metadata head(String bucket, String key);
    void delete(String bucket, String key);
}
