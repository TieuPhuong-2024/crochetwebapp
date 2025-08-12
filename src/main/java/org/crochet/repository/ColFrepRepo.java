package org.crochet.repository;

import org.crochet.model.ColFrep;
import org.crochet.model.Collection;
import org.crochet.model.FreePattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ColFrepRepo extends JpaRepository<ColFrep, String> {
    @Transactional
    @Modifying
    @Query("delete from ColFrep c where c.freePattern.id = :frepId")
    void removeByFreePattern(@Param("frepId") String frepId);

    @Query("""
            SELECT cf.freePattern
            FROM ColFrep cf
            WHERE cf.collection.id = :collectionId
            ORDER BY cf.createdDate DESC
            """)
    List<FreePattern> findFreePatternsByCollectionId(@Param("collectionId") String collectionId);

    @Query("""
            SELECT cf.collection
            FROM ColFrep cf
            WHERE cf.freePattern.id = :frepId
              AND cf.collection.user.id = :userId
            """)
    Optional<Collection> findCollectionByUserAndFreePattern(@Param("userId") String userId, @Param("frepId") String frepId);

    // Query tối ưu để kiểm tra sự tồn tại mà không cần load entity
    @Query("""
            SELECT COUNT(cf.id) > 0
            FROM ColFrep cf
            JOIN cf.collection c
            WHERE cf.freePattern.id = :frepId
              AND c.user.id = :userId
            """)
    boolean existsByFreePatternAndUser(@Param("frepId") String frepId, @Param("userId") String userId);

    // Query tối ưu để lấy count mà không cần load relationship
    @Query("""
            SELECT COUNT(cf.id)
            FROM ColFrep cf
            WHERE cf.collection.id = :collectionId
            """)
    long countByCollectionIdOptimized(@Param("collectionId") String collectionId);

    // Batch delete by multiple free pattern IDs
    @Transactional
    @Modifying
    @Query("DELETE FROM ColFrep cf WHERE cf.freePattern.id IN :frepIds AND cf.collection.user.id = :userId")
    void removeByFreePatternsAndUser(@Param("frepIds") Set<String> frepIds, @Param("userId") String userId);

    // Find collection IDs that contain specific free patterns for a user
    @Query("""
            SELECT DISTINCT cf.collection.id
            FROM ColFrep cf
            WHERE cf.freePattern.id IN :frepIds
              AND cf.collection.user.id = :userId
            """)
    Set<String> findCollectionIdsByFreePatternsAndUser(@Param("frepIds") Set<String> frepIds, @Param("userId") String userId);

    // Check if user has any collections containing specific free patterns
    @Query("""
            SELECT COUNT(cf.id) > 0
            FROM ColFrep cf
            WHERE cf.freePattern.id IN :frepIds
              AND cf.collection.user.id = :userId
            """)
    boolean existsByFreePatternsAndUser(@Param("frepIds") Set<String> frepIds, @Param("userId") String userId);

    // Tối ưu hóa: Sử dụng EXISTS thay vì COUNT cho truy vấn nhanh hơn
    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM ColFrep cf
                JOIN cf.collection c
                WHERE cf.freePattern.id = :frepId
                  AND c.user.id = :userId
            ) THEN true ELSE false END
            """)
    boolean existsByFreePatternAndUserOptimized(@Param("frepId") String frepId, @Param("userId") String userId);

    // Tối ưu hóa: Sử dụng EXISTS cho multiple free patterns
    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM ColFrep cf
                JOIN cf.collection c
                WHERE cf.freePattern.id IN :frepIds
                  AND c.user.id = :userId
            ) THEN true ELSE false END
            """)
    boolean existsByFreePatternsAndUserOptimized(@Param("frepIds") Set<String> frepIds, @Param("userId") String userId);

    // Tối ưu hóa: COUNT nhanh cho collection
    @Query("""
            SELECT COUNT(1)
            FROM ColFrep cf
            WHERE cf.collection.id = :collectionId
            """)
    long countByCollectionIdFast(@Param("collectionId") String collectionId);

    // Tối ưu hóa: Lấy tất cả ColFrep cho nhiều free patterns của user
    @Query("""
            SELECT cf
            FROM ColFrep cf
            JOIN cf.collection c
            WHERE cf.freePattern.id IN :frepIds
              AND c.user.id = :userId
            """)
    List<ColFrep> findByFreePatternIdsAndUser(@Param("frepIds") Set<String> frepIds, @Param("userId") String userId);
}
