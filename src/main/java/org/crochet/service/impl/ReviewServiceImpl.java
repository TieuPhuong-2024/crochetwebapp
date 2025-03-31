package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.ReviewMapper;
import org.crochet.model.Review;
import org.crochet.model.User;
import org.crochet.payload.request.ReviewRequest;
import org.crochet.payload.response.FreePatternResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ReviewResponse;
import org.crochet.repository.FreePatternRepository;
import org.crochet.repository.ReviewRepository;
import org.crochet.repository.UserRepository;
import org.crochet.service.ReviewService;
import org.crochet.util.ObjectUtils;
import org.crochet.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final FreePatternRepository freePatternRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public ReviewResponse createOrUpdate(ReviewRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code());
        }

        // Kiểm tra free pattern có tồn tại không
        var freePattern = freePatternRepository.findById(request.getFreePatternId()).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()));

        Review review;
        if (ObjectUtils.hasText(request.getId())) {
            // Cập nhật đánh giá đã tồn tại
            review = reviewRepository.findById(request.getId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_REVIEW_NOT_FOUND.message(),
                            ResultCode.MSG_REVIEW_NOT_FOUND.code()));

            // Đảm bảo người dùng chỉ có thể cập nhật đánh giá của chính họ
            if (!review.getUser().getId().equals(currentUser.getId()) && !SecurityUtils.hasRole("ROLE_ADMIN")) {
                throw new ResourceNotFoundException(
                        ResultCode.MSG_FORBIDDEN.message(),
                        ResultCode.MSG_FORBIDDEN.code());
            }
        } else {
            // Kiểm tra xem người dùng đã đánh giá free pattern này chưa
            Optional<Review> existingReview = reviewRepository.findByFreePatternIdAndUserId(
                    request.getFreePatternId(), currentUser.getId());

            if (existingReview.isPresent()) {
                // Nếu đã tồn tại, cập nhật đánh giá đó
                review = existingReview.get();
            } else {
                // Tạo đánh giá mới
                review = Review.builder()
                        .freePattern(freePattern)
                        .user(currentUser)
                        .build();
            }
        }

        // Cập nhật thông tin đánh giá
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());

        // Lưu đánh giá
        review = reviewRepository.save(review);

        // Chuyển đổi sang ReviewResponse
        return ReviewMapper.INSTANCE.toResponse(review);
    }

    @Override
    public PaginationResponse<ReviewResponse> getReviewsByFreePattern(String freePatternId, int pageNo, int pageSize) {
        // Kiểm tra free pattern có tồn tại không
        if (!freePatternRepository.existsById(freePatternId)) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                    ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code());
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Review> reviewPage = reviewRepository.findByFreePatternIdOrderByCreatedDateDesc(freePatternId, pageable);

        List<ReviewResponse> reviewResponses = reviewPage.getContent().stream()
                .map(ReviewMapper.INSTANCE::toResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<ReviewResponse>builder()
                .contents(reviewResponses)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    @Override
    public ReviewResponse getUserReviewForFreePattern(String freePatternId, String userId) {
        Optional<Review> review = reviewRepository.findByFreePatternIdAndUserId(freePatternId, userId);
        return review.map(ReviewMapper.INSTANCE::toResponse).orElse(null);
    }

    @Transactional
    @Override
    public void deleteReview(String reviewId) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code());
        }

        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_REVIEW_NOT_FOUND.message(),
                        ResultCode.MSG_REVIEW_NOT_FOUND.code()));

        // Đảm bảo người dùng chỉ có thể xóa đánh giá của chính họ hoặc là admin
        if (!review.getUser().getId().equals(currentUser.getId()) && !SecurityUtils.hasRole("ROLE_ADMIN")) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_FORBIDDEN.message(),
                    ResultCode.MSG_FORBIDDEN.code());
        }

        reviewRepository.delete(review);
    }

    @Override
    public Double getAverageRating(String freePatternId) {
        return reviewRepository.getAverageRatingByFreePatternId(freePatternId);
    }

    @Override
    public PaginationResponse<ReviewResponse> getUserReviews(String userId, int pageNo, int pageSize) {
        // Kiểm tra user có tồn tại không
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code());
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);

        List<ReviewResponse> reviewResponses = reviewPage.getContent().stream()
                .map(ReviewMapper.INSTANCE::toResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<ReviewResponse>builder()
                .contents(reviewResponses)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    @Override
    public List<ReviewResponse> getRecentUserReviews(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Review> reviews = reviewRepository.findRecentReviewsByUserId(userId, pageable);
        return reviews.stream()
                .map(ReviewMapper.INSTANCE::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void getReviewInfoForFreePattern(String freePatternId, FreePatternResponse response) {
        // Lấy rating trung bình
        Double averageRating = getAverageRating(freePatternId);
        response.setAverageRating(averageRating != null ? averageRating : 0.0);

        // Đếm số lượng reviews
        long reviewCount = reviewRepository.countByFreePatternId(freePatternId);
        response.setReviewCount((int) reviewCount);

        // Lấy một số review tiêu biểu (ví dụ: 3 review mới nhất)
        if (reviewCount > 0) {
            Pageable pageable = PageRequest.of(0, 3);
            Page<Review> topReviews = reviewRepository.findByFreePatternIdOrderByCreatedDateDesc(freePatternId,
                    pageable);

            List<ReviewResponse> topReviewResponses = topReviews.getContent().stream()
                    .map(ReviewMapper.INSTANCE::toResponse)
                    .collect(Collectors.toList());

            response.setTopReviews(topReviewResponses);
        }
    }
}