package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.payload.request.ReviewRequest;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.payload.response.ReviewResponse;
import org.crochet.service.ReviewService;
import org.crochet.util.ResponseUtil;
import org.crochet.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Review API", description = "API for managing reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(summary = "Create or update a review")
    @ApiResponse(responseCode = "201", description = "Review created or updated successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReviewResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<ReviewResponse> createOrUpdateReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createOrUpdate(request);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get reviews for a free pattern")
    @ApiResponse(responseCode = "200", description = "List of reviews retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/free-pattern/{freePatternId}")
    public ResponseData<PaginationResponse<ReviewResponse>> getReviewsByFreePattern(
            @Parameter(description = "ID of the free pattern")
            @PathVariable String freePatternId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginationResponse<ReviewResponse> response = reviewService.getReviewsByFreePattern(freePatternId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get current user's review for a free pattern")
    @ApiResponse(responseCode = "200", description = "User's review retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReviewResponse.class)))
    @GetMapping("/me/free-pattern/{freePatternId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<ReviewResponse> getCurrentUserReviewForFreePattern(
            @Parameter(description = "ID of the free pattern")
            @PathVariable String freePatternId) {
        var currentUser = SecurityUtils.getCurrentUser();
        ReviewResponse response = reviewService.getUserReviewForFreePattern(freePatternId, currentUser.getId());
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Delete a review")
    @ApiResponse(responseCode = "200", description = "Review deleted successfully")
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<String> deleteReview(
            @Parameter(description = "ID of the review to delete")
            @PathVariable String reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseUtil.success(ResultCode.MSG_DELETE_SUCCESS.message());
    }

    @Operation(summary = "Get average rating for a free pattern")
    @ApiResponse(responseCode = "200", description = "Average rating retrieved successfully")
    @GetMapping("/average-rating/{freePatternId}")
    public ResponseData<Double> getAverageRating(
            @Parameter(description = "ID of the free pattern")
            @PathVariable String freePatternId) {
        Double averageRating = reviewService.getAverageRating(freePatternId);
        return ResponseUtil.success(averageRating != null ? averageRating : 0.0);
    }

    @Operation(summary = "Get reviews by user")
    @ApiResponse(responseCode = "200", description = "List of user reviews retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/user/{userId}")
    public ResponseData<PaginationResponse<ReviewResponse>> getUserReviews(
            @Parameter(description = "ID of the user")
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginationResponse<ReviewResponse> response = reviewService.getUserReviews(userId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get recent reviews by user")
    @ApiResponse(responseCode = "200", description = "List of recent user reviews retrieved successfully")
    @GetMapping("/user/{userId}/recent")
    public ResponseData<List<ReviewResponse>> getRecentUserReviews(
            @Parameter(description = "ID of the user")
            @PathVariable String userId,
            @Parameter(description = "Number of reviews to retrieve")
            @RequestParam(defaultValue = "5") int limit) {
        List<ReviewResponse> response = reviewService.getRecentUserReviews(userId, limit);
        return ResponseUtil.success(response);
    }
} 