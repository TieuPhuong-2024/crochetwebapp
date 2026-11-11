package org.crochet.payload.response;

import java.time.Instant;

public record PresignedUploadResponse(String objectKey, String publicUrl, String signedUrl, Instant expiresAt) {
}
