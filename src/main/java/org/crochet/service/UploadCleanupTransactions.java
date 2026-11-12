package org.crochet.service;

import org.crochet.model.UploadAsset;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class UploadCleanupTransactions {
    public static final Duration RETRY_LEASE = Duration.ofMinutes(5);
    public record Claim(String objectKey, String bucket, String token) { }

    private final UploadAssetRepository repository;
    private final R2Properties properties;
    private final Clock clock;

    public UploadCleanupTransactions(UploadAssetRepository repository, R2Properties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Claim> claim(String key) {
        properties.requireConfigured();
        properties.validateScope(properties.getBucketName(), key);
        return repository.lockByObjectKey(key).filter(asset -> key.equals(asset.getObjectKey()))
                .filter(this::inScope).filter(asset -> asset.canClaim(clock.instant()))
                .map(asset -> {
                    String token = UUID.randomUUID().toString();
                    var now = clock.instant();
                    asset.claim(now, now.plus(RETRY_LEASE), token);
                    return new Claim(key, asset.getBucketName(), token);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Claim claim) {
        properties.requireConfigured();
        properties.validateScope(claim.bucket(), claim.objectKey());
        repository.lockByObjectKey(claim.objectKey()).filter(asset -> claim.objectKey().equals(asset.getObjectKey()))
                .filter(this::inScope).ifPresent(asset -> asset.deleted(claim.token()));
    }

    private boolean inScope(UploadAsset asset) {
        return properties.getEnvironment().equals(asset.getEnvironment())
                && properties.getBucketName().equals(asset.getBucketName())
                && asset.getObjectKey().startsWith(properties.getEnvironment() + "/");
    }
}
