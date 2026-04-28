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
  Optional<Collection> findCollectionByUserAndFreePattern(@Param("userId") String userId,
      @Param("frepId") String frepId);

  @Transactional
  @Modifying
  @Query("delete from ColFrep c where c.freePattern.id = :frepId and c.collection.id = :collectionId")
  void removeByFreePatternAndCollectionId(@Param("frepId") String frepId,
      @Param("collectionId") String collectionId);

  @Query("""
      SELECT CASE WHEN EXISTS (
          SELECT 1 FROM ColFrep cf
          JOIN cf.collection c
          WHERE cf.freePattern.id = :frepId
            AND c.user.id = :userId
      ) THEN true ELSE false END
      """)
  boolean existsByFreePatternAndUserOptimized(@Param("frepId") String frepId, @Param("userId") String userId);

  @Query("""
      SELECT COUNT(1)
      FROM ColFrep cf
      WHERE cf.collection.id = :collectionId
      """)
  long countByCollectionIdFast(@Param("collectionId") String collectionId);

  /**
   * Check if a collection has any free patterns
   *
   * @param collectionId collection id
   * @return true if the collection has any free patterns
   */
  boolean existsByCollectionId(String collectionId);

  @Query(value = """
      SELECT fpi.file_content
      FROM collection_free_pattern cf
      JOIN free_pattern_image fpi ON fpi.free_pattern_id = cf.free_pattern_id
      WHERE cf.collection_id = :collectionId
      ORDER BY cf.created_date DESC, fpi.display_order ASC
      LIMIT 1
      """, nativeQuery = true)
  Optional<String> findFirstAvatarUrlByCollectionId(@Param("collectionId") String collectionId);

  @Query("""
      SELECT fp.id, CASE WHEN EXISTS (
          SELECT 1 FROM ColFrep cf
          JOIN cf.collection c
          WHERE cf.freePattern.id = fp.id
            AND c.user.id = :userId
      ) THEN true ELSE false END
      FROM FreePattern fp
      WHERE fp.id IN :frepIds
      """)
  List<Object[]> existFreePatternsInCollection(@Param("frepIds") Set<String> frepIds,
      @Param("userId") String userId);
}
