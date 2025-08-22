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

    // Tối ưu hóa: COUNT nhanh cho collection
    @Query("""
            SELECT COUNT(1)
            FROM ColFrep cf
            WHERE cf.collection.id = :collectionId
            """)
    long countByCollectionIdFast(@Param("collectionId") String collectionId);

    // Tối ưu hóa: Lấy trực tiếp các pattern IDs đã được thêm vào collection của user
    @Query("""
            SELECT DISTINCT cf.freePattern.id
            FROM ColFrep cf
            JOIN cf.collection c
            WHERE cf.freePattern.id IN :frepIds
              AND c.user.id = :userId
            """)
    Set<String> findPatternIdsInUserCollections(@Param("frepIds") Set<String> frepIds, @Param("userId") String userId);
}
