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

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    @Query("""
            SELECT new org.crochet.payload.response.CommentResponse(c.id, c.content, c.createdDate)
            FROM Comment c
            LEFT JOIN User u ON c.user.id = u.id
            WHERE u.id = :userId
            ORDER BY c.createdDate DESC
            LIMIT 5
            """)
    List<CommentResponse> getRecentCommentsByUserId(@Param("userId") String userId);
    
    // Lấy tất cả root comments (không có parent) cho một bài viết, hỗ trợ phân trang
    Page<Comment> findByBlogPostIdAndParentIsNullOrderByCreatedDateDesc(String blogPostId, Pageable pageable);
    
    // Lấy tất cả replies cho một comment cụ thể
    List<Comment> findByParentIdOrderByCreatedDateAsc(String parentId);
    
    // Lấy tất cả comments (cả root và replies) cho một bài viết
    Page<Comment> findByBlogPostIdOrderByCreatedDateDesc(String blogPostId, Pageable pageable);
    
    // Đếm số lượng replies cho một comment
    long countByParentId(String parentId);
    
    // Đếm số lượng root comments cho một bài viết
    long countByBlogPostIdAndParentIsNull(String blogPostId);
    
    // Đếm số lượng comments cho một bài viết
    long countByBlogPostId(String blogPostId);
}