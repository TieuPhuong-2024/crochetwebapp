package org.crochet.repository;

import jakarta.persistence.LockModeType;
import org.crochet.model.UploadAsset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UploadAssetRepository extends JpaRepository<UploadAsset, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UploadAsset a where a.objectKey = :key")
    Optional<UploadAsset> lockByObjectKey(@Param("key") String key);

    @Query("""
            select a.objectKey from UploadAsset a
            where a.environment = :environment and a.bucketName = :bucket
              and a.state in :states and a.expiresAt <= :now
              and (a.nextAttemptAt is null or a.nextAttemptAt <= :now)
            order by a.expiresAt, a.objectKey
            """)
    List<String> findCleanupCandidates(@Param("environment") String environment, @Param("bucket") String bucket,
                                       @Param("states") Collection<UploadAsset.State> states,
                                       @Param("now") Instant now, Pageable pageable);
}
