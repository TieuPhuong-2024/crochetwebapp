package org.crochet.service;

import lombok.extern.slf4j.Slf4j;
import org.crochet.model.UploadAsset;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@Slf4j
public class UploadCleanupService {
    private final UploadAssetRepository repository;
    private final UploadCleanupTransactions transactions;
    private final R2Storage storage;
    private final R2Properties properties;
    private final Clock clock;

    public UploadCleanupService(UploadAssetRepository repository, UploadCleanupTransactions transactions,
                                R2Storage storage, R2Properties properties, Clock clock) {
        this.repository = repository;
        this.transactions = transactions;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 300000)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void cleanupExpired() {
        if (!properties.isConfigured()) {
            return;
        }
        var keys = repository.findCleanupCandidates(properties.getEnvironment(), properties.getBucketName(),
                List.of(UploadAsset.State.PENDING, UploadAsset.State.DELETING), clock.instant(), PageRequest.of(0, 100));
        for (String key : keys) {
            try {
                var claim = transactions.claim(key);
                if (claim.isPresent()) {
                    // No database transaction is held during the network call. The claim is already committed.
                    var claimed = claim.get();
                    storage.delete(claimed.bucket(), claimed.objectKey());
                    transactions.complete(claimed);
                }
            } catch (RuntimeException ex) {
                // Keep DELETING and its lease for retry; never log storage URLs, keys or SDK exceptions.
                log.warn("Upload cleanup attempt failed; it will be retried after its lease");
            }
        }
    }
}
