package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Tạo hoặc cập nhật comment")
    @ApiResponse(responseCode = "201", description = "Comment created successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CommentResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<CommentResponse> createComment(
            @Valid @RequestBody CommentRequest request) {
        var response = commentService.createOrUpdate(request);
        return ResponseUtil.success(response);
    }
    
    @Operation(summary = "Lấy danh sách root comments cho một bài viết")
    @ApiResponse(responseCode = "200", description = "Root comments retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/root/{blogId}")
    public ResponseData<PaginationResponse<CommentResponse>> getRootCommentsByBlogPost(
            @PathVariable("blogId") String blogId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
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
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        var response = commentService.getCommentsByBlogPost(blogId, pageNo, pageSize);
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
    
    @Operation(summary = "Đếm số lượng root comments cho một bài viết")
    @ApiResponse(responseCode = "200", description = "Root comment count retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Long.class)))
    @GetMapping("/count/root/{blogId}")
    public ResponseData<Long> countRootCommentsByBlogPost(@PathVariable("blogId") String blogId) {
        long count = commentService.countRootCommentsByBlogPost(blogId);
        return ResponseUtil.success(count);
    }
    
    @Operation(summary = "Đếm số lượng tất cả comments cho một bài viết")
    @ApiResponse(responseCode = "200", description = "Comment count retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Long.class)))
    @GetMapping("/count/{blogId}")
    public ResponseData<Long> countCommentsByBlogPost(@PathVariable("blogId") String blogId) {
        long count = commentService.countCommentsByBlogPost(blogId);
        return ResponseUtil.success(count);
    }
}
