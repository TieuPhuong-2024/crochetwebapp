package org.crochet.service;

import org.crochet.payload.request.CommentRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.payload.response.PaginationResponse;

import java.util.List;

public interface CommentService {
    CommentResponse createOrUpdate(CommentRequest request);
    
    // Lấy danh sách root comments cho một bài viết (không bao gồm replies)
    PaginationResponse<CommentResponse> getRootCommentsByBlogPost(String blogPostId, int pageNo, int pageSize);
    
    // Lấy tất cả comments cho một bài viết (cả root và replies)
    PaginationResponse<CommentResponse> getCommentsByBlogPost(String blogPostId, int pageNo, int pageSize);
    
    // Lấy danh sách replies cho một comment cụ thể
    List<CommentResponse> getRepliesByCommentId(String commentId);
    
    // Xóa một comment
    void deleteComment(String commentId);
    
    // Đếm số lượng root comments cho một bài viết
    long countRootCommentsByBlogPost(String blogPostId);
    
    // Đếm số lượng tất cả comments cho một bài viết
    long countCommentsByBlogPost(String blogPostId);
}
