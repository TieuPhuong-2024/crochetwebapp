package org.crochet.repository;

import org.crochet.model.Comment;
import org.crochet.payload.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing Comment entities.
 * Provides methods for retrieving, counting, and managing comments for blog posts, products, and free patterns.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    /**
     * Retrieves the 5 most recent comments made by a specific user.
     *
     * @param userId The ID of the user whose comments to retrieve
     * @return A list of CommentResponse objects containing comment details
     */
    @Query("""
            SELECT new org.crochet.payload.response.CommentResponse(c.id, c.content, c.createdAt)
            FROM Comment c
            WHERE c.user.id = :userId
            ORDER BY c.createdAt DESC
            LIMIT 5
            """)
    List<CommentResponse> getRecentCommentsByUserId(@Param("userId") String userId);

    /**
     * Retrieves all replies for a specific parent comment, ordered by creation date (ascending).
     *
     * @param parentId The ID of the parent comment
     * @return A list of reply comments
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(String parentId);

    /**
     * Counts the number of replies for a specific parent comment.
     *
     * @param parentId The ID of the parent comment
     * @return The number of replies
     */
    long countByParentId(String parentId);
    
    /**
     * Product comments
     * Retrieves all root comments (comments without a parent) for a specific product with pagination support.
     *
     * @param productId The ID of the product
     * @param pageable Pagination information
     * @return A page of root comments for the specified product
     */
    Page<Comment> findByProductIdAndParentIsNullOrderByCreatedAtDesc(String productId, Pageable pageable);
    
    /**
     * Retrieves all comments (both root comments and replies) for a specific product with pagination support.
     *
     * @param productId The ID of the product
     * @param pageable Pagination information
     * @return A page of all comments for the specified product
     */
    Page<Comment> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    
    /**
     * Counts the number of root comments (comments without a parent) for a specific product.
     *
     * @param productId The ID of the product
     * @return The number of root comments
     */
    long countByProductIdAndParentIsNull(String productId);
    
    /**
     * Counts the total number of comments (both root comments and replies) for a specific product.
     *
     * @param productId The ID of the product
     * @return The total number of comments
     */
    long countByProductId(String productId);
    
    /**
     * Free Pattern comments
     * Retrieves all root comments (comments without a parent) for a specific free pattern with pagination support.
     *
     * @param freePatternId The ID of the free pattern
     * @param pageable Pagination information
     * @return A page of root comments for the specified free pattern
     */
    Page<Comment> findByFreePatternIdAndParentIsNullOrderByCreatedAtDesc(String freePatternId, Pageable pageable);
    
    /**
     * Retrieves all comments (both root comments and replies) for a specific free pattern with pagination support.
     *
     * @param freePatternId The ID of the free pattern
     * @param pageable Pagination information
     * @return A page of all comments for the specified free pattern
     */
    Page<Comment> findByFreePatternIdOrderByCreatedAtDesc(String freePatternId, Pageable pageable);
    
    /**
     * Counts the number of root comments (comments without a parent) for a specific free pattern.
     *
     * @param freePatternId The ID of the free pattern
     * @return The number of root comments
     */
    long countByFreePatternIdAndParentIsNull(String freePatternId);
    
    /**
     * Counts the total number of comments (both root comments and replies) for a specific free pattern.
     *
     * @param freePatternId The ID of the free pattern
     * @return The total number of comments
     */
    long countByFreePatternId(String freePatternId);
}