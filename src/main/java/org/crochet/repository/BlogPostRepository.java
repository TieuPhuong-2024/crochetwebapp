package org.crochet.repository;

import org.crochet.model.BlogPost;
import org.crochet.payload.response.BlogPostResponse;
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
public interface BlogPostRepository extends JpaRepository<BlogPost, String>, JpaSpecificationExecutor<BlogPost> {

    @Query("""
            SELECT
              new org.crochet.payload.response.BlogPostResponse (
                p.id,
                p.title,
                p.content,
                f.fileContent,
                p.createdDate,
                u.id,
                u.name,
                u.imageUrl,
                p.viewCount,
                p.likeCount
              )
            FROM
              BlogPost p
              JOIN p.files f WITH f.order = 0
              JOIN User u on u.id = p.createdBy
            WHERE
              p.home = TRUE
            """)
    List<BlogPostResponse> findLimitedNumPosts(Pageable pageable);

    @Query("""
            SELECT
              p
            FROM
              BlogPost p
              LEFT JOIN FETCH p.files
            WHERE
              p.id = :id
            """)
    Optional<BlogPost> getDetail(@Param("id") String id);

    @Query("""
            SELECT
              new org.crochet.payload.response.BlogPostResponse (
                p.id,
                p.title,
                p.content,
                f.fileContent,
                p.createdDate,
                u.id,
                u.name,
                u.imageUrl,
                p.viewCount,
                p.likeCount
              )
            FROM
              BlogPost p
              LEFT JOIN p.files f WITH f.order = 0
              LEFT JOIN User u on u.id = p.createdBy
            WHERE
              p.id IN :ids
            """)
    Page<BlogPostResponse> findPostWithIds(@Param("ids") List<String> ids, Pageable pageable);

    @Query("""
            SELECT
              new org.crochet.payload.response.BlogPostResponse (
                p.id,
                p.title,
                p.content,
                f.fileContent,
                p.createdDate,
                u.id,
                u.name,
                u.imageUrl,
                p.viewCount,
                p.likeCount
              )
            FROM
              BlogPost p
              LEFT JOIN p.files f WITH f.order = 0
              LEFT JOIN User u on u.id = p.createdBy
            """)
    Page<BlogPostResponse> findPostWithPageable(Pageable pageable);

    @Query("""
            SELECT
              p.id
            FROM
              BlogPost p
            ORDER BY
              p.createdDate DESC
            """)
    List<String> getBlogIds(Pageable pageable);

    @Modifying
    @Query("UPDATE BlogPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE BlogPost p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE BlogPost p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementLikeCount(@Param("id") String id);
}
