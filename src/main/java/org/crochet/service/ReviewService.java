package org.crochet.service;

import org.crochet.payload.request.ReviewRequest;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ReviewResponse;
import org.crochet.payload.response.FreePatternResponse;

import java.util.List;

public interface ReviewService {
    /**
     * Tạo mới hoặc cập nhật đánh giá cho free pattern
     *
     * @param request Thông tin đánh giá
     * @return ReviewResponse
     */
    ReviewResponse createOrUpdate(ReviewRequest request);
    
    /**
     * Lấy danh sách đánh giá của một free pattern
     *
     * @param freePatternId ID của free pattern
     * @param pageNo Số trang
     * @param pageSize Kích thước trang
     * @return PaginationResponse chứa danh sách đánh giá
     */
    PaginationResponse<ReviewResponse> getReviewsByFreePattern(String freePatternId, int pageNo, int pageSize);
    
    /**
     * Lấy đánh giá của người dùng cho một free pattern
     *
     * @param freePatternId ID của free pattern
     * @param userId ID của người dùng
     * @return ReviewResponse hoặc null nếu không tìm thấy
     */
    ReviewResponse getUserReviewForFreePattern(String freePatternId, String userId);
    
    /**
     * Xóa đánh giá
     *
     * @param reviewId ID của đánh giá cần xóa
     */
    void deleteReview(String reviewId);
    
    /**
     * Lấy rating trung bình của một free pattern
     *
     * @param freePatternId ID của free pattern
     * @return Rating trung bình (từ 1-5)
     */
    Double getAverageRating(String freePatternId);
    
    /**
     * Lấy danh sách đánh giá của một người dùng
     *
     * @param userId ID của người dùng
     * @param pageNo Số trang
     * @param pageSize Kích thước trang
     * @return PaginationResponse chứa danh sách đánh giá
     */
    PaginationResponse<ReviewResponse> getUserReviews(String userId, int pageNo, int pageSize);
    
    /**
     * Lấy các đánh giá gần đây của một người dùng
     *
     * @param userId ID của người dùng
     * @param limit Số lượng đánh giá tối đa cần lấy
     * @return Danh sách đánh giá
     */
    List<ReviewResponse> getRecentUserReviews(String userId, int limit);
    
    /**
     * Bổ sung thông tin đánh giá vào FreePatternResponse
     *
     * @param freePatternId ID của free pattern
     * @param response FreePatternResponse cần bổ sung thông tin
     */
    void getReviewInfoForFreePattern(String freePatternId, FreePatternResponse response);
} 