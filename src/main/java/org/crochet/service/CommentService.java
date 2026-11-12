package org.crochet.service;

import org.crochet.payload.request.CommentRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.payload.response.PaginationResponse;

import java.util.List;

public interface CommentService {
    CommentResponse createOrUpdate(CommentRequest request);
    
    // Blog Post Comments
    // Lấy danh sách root comments cho một bài viết (không bao gồm replies)
    PaginationResponse<CommentResponse> getRootCommentsByBlogPost(String blogPostId, int pageNo, int pageSize);
    
    // Lấy tất cả comments cho một bài viết (cả root và replies)
    PaginationResponse<CommentResponse> getCommentsByBlogPost(String blogPostId, int pageNo, int pageSize);
    
    // Đếm số lượng root comments cho một bài viết
    long countRootCommentsByBlogPost(String blogPostId);
    
    // Đếm số lượng tất cả comments cho một bài viết
    long countCommentsByBlogPost(String blogPostId);
    
    // Product Comments
    // Lấy danh sách root comments cho một product (không bao gồm replies)
    PaginationResponse<CommentResponse> getRootCommentsByProduct(String productId, int pageNo, int pageSize);
    
    // Lấy tất cả comments cho một product (cả root và replies)
    PaginationResponse<CommentResponse> getCommentsByProduct(String productId, int pageNo, int pageSize);
    
    // Đếm số lượng root comments cho một product
    long countRootCommentsByProduct(String productId);
    
    // Đếm số lượng tất cả comments cho một product
    long countCommentsByProduct(String productId);
    
    // Free Pattern Comments
    // Lấy danh sách root comments cho một free pattern (không bao gồm replies)
    PaginationResponse<CommentResponse> getRootCommentsByFreePattern(String freePatternId, int pageNo, int pageSize);
    
    // Lấy tất cả comments cho một free pattern (cả root và replies)
    PaginationResponse<CommentResponse> getCommentsByFreePattern(String freePatternId, int pageNo, int pageSize);
    
    // Đếm số lượng root comments cho một free pattern
    long countRootCommentsByFreePattern(String freePatternId);
    
    // Đếm số lượng tất cả comments cho một free pattern
    long countCommentsByFreePattern(String freePatternId);
    
    // Common methods
    // Lấy danh sách replies cho một comment cụ thể
    List<CommentResponse> getRepliesByCommentId(String commentId);
    
    // Xóa một comment
    void deleteComment(String commentId);
}
