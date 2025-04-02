package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.crochet.payload.request.CommentRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.service.CommentService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "Quản lý comments")
public class CommentController {
    private final CommentService commentService;

    @Operation(summary = "Tạo hoặc cập nhật comment")
    @ApiResponse(responseCode = "201", description = "Comment created successfully", 
            content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = CommentResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<CommentResponse> createOrUpdateComment(
            @Valid @RequestBody CommentRequest request) {
        var response = commentService.createOrUpdate(request);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách root comments cho một bài viết")
    @ApiResponse(responseCode = "200", description = "Root comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/blog/{blogId}/root")
    public ResponseData<PaginationResponse<CommentResponse>> getRootCommentsByBlogPost(
            @PathVariable("blogId") String blogId,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        var response = commentService.getRootCommentsByBlogPost(blogId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách tất cả comments cho một bài viết")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/blog/{blogId}")
    public ResponseData<PaginationResponse<CommentResponse>> getCommentsByBlogPost(
            @PathVariable("blogId") String blogId,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        var response = commentService.getCommentsByBlogPost(blogId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách root comments cho một sản phẩm")
    @ApiResponse(responseCode = "200", description = "Root comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/product/{productId}/root")
    public ResponseData<PaginationResponse<CommentResponse>> getRootCommentsByProduct(
            @PathVariable("productId") String productId,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        var response = commentService.getRootCommentsByProduct(productId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách tất cả comments cho một sản phẩm")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/product/{productId}")
    public ResponseData<PaginationResponse<CommentResponse>> getCommentsByProduct(
            @PathVariable("productId") String productId,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        var response = commentService.getCommentsByProduct(productId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách root comments cho một free pattern")
    @ApiResponse(responseCode = "200", description = "Root comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/free-pattern/{freePatternId}/root")
    public ResponseData<PaginationResponse<CommentResponse>> getRootCommentsByFreePattern(
            @PathVariable("freePatternId") String freePatternId,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        var response = commentService.getRootCommentsByFreePattern(freePatternId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách tất cả comments cho một free pattern")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/free-pattern/{freePatternId}")
    public ResponseData<PaginationResponse<CommentResponse>> getCommentsByFreePattern(
            @PathVariable("freePatternId") String freePatternId,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        var response = commentService.getCommentsByFreePattern(freePatternId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách replies cho một comment")
    @ApiResponse(responseCode = "200", description = "Replies retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CommentResponse.class)))
    @GetMapping("/replies/{commentId}")
    public ResponseData<List<CommentResponse>> getRepliesByCommentId(
            @PathVariable("commentId") String commentId) {
        var response = commentService.getRepliesByCommentId(commentId);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Xóa một comment")
    @ApiResponse(responseCode = "204", description = "Comment deleted successfully")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<Void> deleteComment(@PathVariable("id") String id) {
        commentService.deleteComment(id);
        return ResponseUtil.success();
    }

    @Operation(summary = "Count root comments cho một blog post")
    @ApiResponse(responseCode = "200", description = "Count root comments successfully")
    @GetMapping("/blog/{blogId}/root/count")
    public ResponseData<Long> countRootCommentsByBlogPost(@PathVariable("blogId") String blogId) {
        long count = commentService.countRootCommentsByBlogPost(blogId);
        return ResponseUtil.success(count);
    }

    @Operation(summary = "Count tất cả comments cho một blog post")
    @ApiResponse(responseCode = "200", description = "Count all comments successfully")
    @GetMapping("/blog/{blogId}/count")
    public ResponseData<Long> countCommentsByBlogPost(@PathVariable("blogId") String blogId) {
        long count = commentService.countCommentsByBlogPost(blogId);
        return ResponseUtil.success(count);
    }

    @Operation(summary = "Count root comments cho một sản phẩm")
    @ApiResponse(responseCode = "200", description = "Count root comments successfully")
    @GetMapping("/product/{productId}/root/count")
    public ResponseData<Long> countRootCommentsByProduct(@PathVariable("productId") String productId) {
        long count = commentService.countRootCommentsByProduct(productId);
        return ResponseUtil.success(count);
    }

    @Operation(summary = "Count tất cả comments cho một sản phẩm")
    @ApiResponse(responseCode = "200", description = "Count all comments successfully")
    @GetMapping("/product/{productId}/count")
    public ResponseData<Long> countCommentsByProduct(@PathVariable("productId") String productId) {
        long count = commentService.countCommentsByProduct(productId);
        return ResponseUtil.success(count);
    }

    @Operation(summary = "Count root comments cho một free pattern")
    @ApiResponse(responseCode = "200", description = "Count root comments successfully")
    @GetMapping("/free-pattern/{freePatternId}/root/count")
    public ResponseData<Long> countRootCommentsByFreePattern(@PathVariable("freePatternId") String freePatternId) {
        long count = commentService.countRootCommentsByFreePattern(freePatternId);
        return ResponseUtil.success(count);
    }

    @Operation(summary = "Count tất cả comments cho một free pattern")
    @ApiResponse(responseCode = "200", description = "Count all comments successfully")
    @GetMapping("/free-pattern/{freePatternId}/count")
    public ResponseData<Long> countCommentsByFreePattern(@PathVariable("freePatternId") String freePatternId) {
        long count = commentService.countCommentsByFreePattern(freePatternId);
        return ResponseUtil.success(count);
    }
}
