package org.crochet.repository;

import org.crochet.model.Pattern;
import org.crochet.payload.response.PatternResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatternRepository extends JpaRepository<Pattern, String>, JpaSpecificationExecutor<Pattern> {

    @Query("""
            SELECT
              p
            FROM
              Pattern p
              JOIN FETCH p.category
            WHERE
              p.id =:id
            """)
    Optional<Pattern> findPatternById(@Param("id") String id);

    @Query("""
            SELECT
              new org.crochet.payload.response.PatternResponse (
                p.id,
                p.name,
                p.description,
                p.price,
                p.currencyCode,
                i.fileContent
              )
            FROM
              Pattern p
              LEFT JOIN p.images i WITH i.order = 0
            WHERE
              p.isHome = TRUE
            """)
    List<PatternResponse> findLimitedNumPattern(Pageable pageable);

    @Query("""
            SELECT
              new org.crochet.payload.response.PatternResponse (
                p.id,
                p.name,
                p.description,
                p.price,
                p.currencyCode,
                i.fileContent
              )
            FROM
              Pattern p
              LEFT JOIN p.images i WITH i.order = 0
            WHERE
              p.id IN :patternIds
            """)
    Page<PatternResponse> findPatternWithIds(@Param("patternIds") List<String> ids, Pageable pageable);

    @Query("""
            SELECT
              new org.crochet.payload.response.PatternResponse (
                p.id,
                p.name,
                p.description,
                p.price,
                p.currencyCode,
                i.fileContent
              )
            FROM
              Pattern p
              LEFT JOIN p.images i WITH i.order = 0
            """)
    Page<PatternResponse> findPatternWithPageable(Pageable pageable);

    @Query("""
            SELECT
              p.id
            FROM
              Pattern p
            ORDER BY
              p.createdDate DESC
            """)
    List<String> getPatternIds(Pageable pageable);

    @Modifying
    @Query("UPDATE Pattern p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE Pattern p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE Pattern p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementLikeCount(@Param("id") String id);
}