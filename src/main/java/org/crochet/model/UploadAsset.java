package org.crochet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadAsset {
    public enum State { PENDING, ATTACHED, DELETING, DELETED }

    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "object_key", nullable = false, unique = true, length = 255)
    private String objectKey;
    @Column(nullable = false, length = 16)
    private String environment;
    @Column(name = "bucket_name", nullable = false, length = 63)
    private String bucketName;
    @Column(name = "owner_id", nullable = false, length = 50)
    private String ownerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private State state;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "claim_token", length = 36)
    private String claimToken;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long size;

    public UploadAsset(String objectKey, String environment, String bucketName, String ownerId,
                       String contentType, long size, Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.objectKey = objectKey;
        this.environment = environment;
        this.bucketName = bucketName;
        this.ownerId = ownerId;
        this.contentType = contentType;
        this.size = size;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.state = State.PENDING;
    }

    public void attach() {
        if (state != State.PENDING && state != State.ATTACHED) {
            throw new IllegalStateException("Upload cannot be attached");
        }
        state = State.ATTACHED;
    }

    public boolean canClaim(Instant now) {
        return !expiresAt.isAfter(now) && (state == State.PENDING
                || (state == State.DELETING && nextAttemptAt != null && !nextAttemptAt.isAfter(now)));
    }

    public void claim(Instant now, Instant retryAt, String token) {
        if (!canClaim(now)) {
            throw new IllegalStateException("Upload cannot be claimed");
        }
        state = State.DELETING;
        nextAttemptAt = retryAt;
        claimToken = token;
    }

    public void deleted(String token) {
        if (state == State.DELETING && token.equals(claimToken)) {
            state = State.DELETED;
            nextAttemptAt = null;
            claimToken = null;
        }
    }
}
