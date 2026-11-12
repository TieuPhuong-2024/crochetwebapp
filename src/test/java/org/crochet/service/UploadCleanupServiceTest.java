package org.crochet.service;

import org.crochet.model.UploadAsset;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.crochet.service.UploadTestSupport.*;

class UploadCleanupServiceTest {
    private UploadAssetRepository repository;
    private R2Storage storage;
    private R2Properties properties;
    private UploadAsset asset;
    private UploadCleanupTransactions transactions;

    @BeforeEach
    void setUp() {
        repository = mock(UploadAssetRepository.class);
        storage = mock(R2Storage.class);
        properties = properties();
        asset = asset(KEY, NOW);
        when(repository.lockByObjectKey(KEY)).thenReturn(Optional.of(asset));
        transactions = new UploadCleanupTransactions(repository, properties, CLOCK);
    }

    @Test
    void claimRejectsLiveAttachedWrongEnvironmentAndBucket() {
        when(repository.lockByObjectKey(KEY)).thenReturn(Optional.of(asset()));
        assertThat(transactions.claim(KEY)).isEmpty();
        var attached = asset();
        attached.attach();
        when(repository.lockByObjectKey(KEY)).thenReturn(Optional.of(attached));
        assertThat(transactions.claim(KEY)).isEmpty();
        var wrongBucket = new UploadAsset(KEY, "development", "another-bucket", "owner", "image/png", 100, NOW, NOW);
        when(repository.lockByObjectKey(KEY)).thenReturn(Optional.of(wrongBucket));
        assertThat(transactions.claim(KEY)).isEmpty();
        assertThatThrownBy(() -> transactions.claim(KEY.replace("development", "production"))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void leasePreventsDoubleClaimAndStaleCompletionDoesNotWin() {
        var first = transactions.claim(KEY).orElseThrow();
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.DELETING);
        assertThat(asset.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(transactions.claim(KEY)).isEmpty();
        var later = new UploadCleanupTransactions(repository, properties, Clock.fixed(NOW.plusSeconds(300), ZoneOffset.UTC));
        var retry = later.claim(KEY).orElseThrow();
        assertThat(retry.token()).isNotEqualTo(first.token());
        later.complete(first);
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.DELETING);
        later.complete(retry);
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.DELETED);
        assertThat(asset.getNextAttemptAt()).isNull();
        verify(repository, never()).delete(any());
    }

    @Test
    void boundsAndScopesBatchThenRetriesFailureAfterLease() {
        when(repository.findCleanupCandidates(eq("development"), eq("unit-test-bucket"), any(), any(), eq(PageRequest.of(0, 100))))
                .thenReturn(List.of(KEY));
        doThrow(new IllegalStateException("offline")).doNothing().when(storage).delete("unit-test-bucket", KEY);
        var cleanup = new UploadCleanupService(repository, transactions, storage, properties, CLOCK);
        cleanup.cleanupExpired();
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.DELETING);
        cleanup.cleanupExpired();
        verify(storage, times(1)).delete("unit-test-bucket", KEY);
        Clock laterClock = Clock.fixed(NOW.plusSeconds(300), ZoneOffset.UTC);
        new UploadCleanupService(repository, new UploadCleanupTransactions(repository, properties, laterClock),
                storage, properties, laterClock).cleanupExpired();
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.DELETED);
        verify(storage, times(2)).delete("unit-test-bucket", KEY);
    }

    @Test
    void unconfiguredDoesNotQueryOrDeleteAnything() {
        properties.setAccessKey("");
        new UploadCleanupService(repository, transactions, storage, properties, CLOCK).cleanupExpired();
        verifyNoInteractions(repository, storage);
    }
}
