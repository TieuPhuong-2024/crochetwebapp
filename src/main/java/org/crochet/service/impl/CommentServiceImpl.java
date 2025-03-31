package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.CommentMapper;
import org.crochet.model.Comment;
import org.crochet.model.User;
import org.crochet.payload.request.CommentRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.repository.BlogPostRepository;
import org.crochet.repository.CommentRepository;
import org.crochet.repository.UserRepository;
import org.crochet.service.CommentService;
import org.crochet.util.ObjectUtils;
import org.crochet.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * CommentServiceImpl class
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepo;
    private final BlogPostRepository blogPostRepo;
    private final UserRepository userRepository;

    /**
     * Creates a new comment or updates an existing one based on the provided {@link CommentRequest}.
     * If the request contains an ID, it updates the existing comment with the corresponding ID.
     * If the request does not contain an ID, it creates a new comment.
     *
     * @param request The {@link CommentRequest} containing information for creating or updating the comment.
     * @return The {@link CommentResponse} containing information about the created or updated comment.
     * @throws ResourceNotFoundException If an existing comment is to be updated, and the specified ID is not found.
     */
    @Transactional
    @Override
    public CommentResponse createOrUpdate(CommentRequest request) {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }

        var blog = blogPostRepo.findById(request.getBlogPostId()).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_BLOG_NOT_FOUND.message(),
                        ResultCode.MSG_BLOG_NOT_FOUND.code()
                )
        );

        Comment parent = null;
        if (ObjectUtils.hasText(request.getParentId())) {
            parent = commentRepo.findById(request.getParentId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_COMMENT_NOT_FOUND.message(),
                            ResultCode.MSG_COMMENT_NOT_FOUND.code()
                    )
            );
            
            // Kiểm tra parent comment phải thuộc về bài viết này
            if (!parent.getBlogPost().getId().equals(blog.getId())) {
                throw new ResourceNotFoundException(
                        ResultCode.MSG_BLOG_NOT_FOUND.message(),
                        ResultCode.MSG_BLOG_NOT_FOUND.code()
                );
            }
            
            // Đảm bảo chỉ cho phép độ sâu tối đa là 2 (root comment và replies)
            if (parent.getParent() != null) {
                // Nếu parent là reply, thì sử dụng parent của parent
                parent = parent.getParent();
            }
        }

        var id = request.getId();
        Comment comment;
        if (!ObjectUtils.hasText(id)) {
            comment = Comment.builder()
                    .blogPost(blog)
                    .user(user)
                    .parent(parent)
                    .build();
        } else {
            comment = commentRepo.findById(id).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_COMMENT_NOT_FOUND.message(),
                            ResultCode.MSG_COMMENT_NOT_FOUND.code()
                    )
            );
        }
        
        comment.setContent(request.getContent());
        comment.setCreatedDate(LocalDateTime.now());
        
        // Xử lý mention
        if (ObjectUtils.hasText(request.getMentionedUserId())) {
            // Kiểm tra người dùng được mention có tồn tại không
            userRepository.findById(request.getMentionedUserId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_USER_NOT_FOUND.message(),
                            ResultCode.MSG_USER_NOT_FOUND.code()
                    )
            );
            comment.setMentionedUserId(request.getMentionedUserId());
        }
        
        comment = commentRepo.save(comment);
        CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);
        
        // Thêm thông tin về người dùng được mention
        if (ObjectUtils.hasText(comment.getMentionedUserId())) {
            User mentionedUser = userRepository.findById(comment.getMentionedUserId()).orElse(null);
            if (mentionedUser != null) {
                response.setMentionedUsername(mentionedUser.getUsername());
            }
        }
        
        return response;
    }
    
    /**
     * Lấy danh sách root comments cho một bài viết với phân trang
     *
     * @param blogPostId ID của bài viết cần lấy comments
     * @param pageNo Số trang (bắt đầu từ 0)
     * @param pageSize Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách root comments và thông tin phân trang
     */
    @Override
    public PaginationResponse<CommentResponse> getRootCommentsByBlogPost(String blogPostId, int pageNo, int pageSize) {
        // Kiểm tra xem bài viết có tồn tại không
        blogPostRepo.findById(blogPostId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_BLOG_NOT_FOUND.message(),
                        ResultCode.MSG_BLOG_NOT_FOUND.code()
                )
        );
        
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comment> commentPage = commentRepo.findByBlogPostIdAndParentIsNullOrderByCreatedDateDesc(blogPostId, pageable);
        List<CommentResponse> rootComments = new ArrayList<>();
        
        for (Comment comment : commentPage.getContent()) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);
            
            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                User mentionedUser = userRepository.findById(comment.getMentionedUserId()).orElse(null);
                if (mentionedUser != null) {
                    response.setMentionedUsername(mentionedUser.getUsername());
                }
            }
            
            // Đếm số lượng replies
            long replyCount = commentRepo.countByParentId(comment.getId());
            response.setReplyCount(replyCount);
            
            // Lấy một số replies (có thể giới hạn số lượng, ví dụ 2-3 replies đầu tiên)
            if (replyCount > 0) {
                List<Comment> replies = commentRepo.findByParentIdOrderByCreatedDateAsc(comment.getId());
                List<CommentResponse> replyResponses = new ArrayList<>();
                
                for (Comment reply : replies) {
                    CommentResponse replyResponse = CommentMapper.INSTANCE.toResponse(reply);
                    
                    // Thêm thông tin về người dùng được mention trong reply
                    if (ObjectUtils.hasText(reply.getMentionedUserId())) {
                        User mentionedUser = userRepository.findById(reply.getMentionedUserId()).orElse(null);
                        if (mentionedUser != null) {
                            replyResponse.setMentionedUsername(mentionedUser.getUsername());
                        }
                    }
                    
                    replyResponses.add(replyResponse);
                }
                
                response.setReplies(replyResponses);
            } else {
                response.setReplies(new ArrayList<>());
            }
            
            rootComments.add(response);
        }
        
        return PaginationResponse.<CommentResponse>builder()
                .contents(rootComments)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .build();
    }
    
    /**
     * Lấy tất cả các comments cho một bài viết (bao gồm cả root và replies)
     *
     * @param blogPostId ID của bài viết cần lấy comments
     * @param pageNo Số trang (bắt đầu từ 0)
     * @param pageSize Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách comments và thông tin phân trang
     */
    @Override
    public PaginationResponse<CommentResponse> getCommentsByBlogPost(String blogPostId, int pageNo, int pageSize) {
        // Kiểm tra xem bài viết có tồn tại không
        blogPostRepo.findById(blogPostId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_BLOG_NOT_FOUND.message(),
                        ResultCode.MSG_BLOG_NOT_FOUND.code()
                )
        );
        
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comment> commentPage = commentRepo.findByBlogPostIdOrderByCreatedDateDesc(blogPostId, pageable);
        List<CommentResponse> comments = new ArrayList<>();
        
        for (Comment comment : commentPage.getContent()) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);
            
            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                User mentionedUser = userRepository.findById(comment.getMentionedUserId()).orElse(null);
                if (mentionedUser != null) {
                    response.setMentionedUsername(mentionedUser.getUsername());
                }
            }
            
            comments.add(response);
        }
        
        return PaginationResponse.<CommentResponse>builder()
                .contents(comments)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .build();
    }
    
    /**
     * Lấy danh sách replies cho một comment cụ thể
     *
     * @param commentId ID của comment cần lấy replies
     * @return Danh sách các replies
     */
    @Override
    public List<CommentResponse> getRepliesByCommentId(String commentId) {
        
        if (!commentRepo.existsById(commentId)) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_COMMENT_NOT_FOUND.message(),
                    ResultCode.MSG_COMMENT_NOT_FOUND.code()
            );
        }
        
        List<Comment> replies = commentRepo.findByParentIdOrderByCreatedDateAsc(commentId);
        List<CommentResponse> replyResponses = new ArrayList<>();
        
        for (Comment reply : replies) {
            CommentResponse replyResponse = CommentMapper.INSTANCE.toResponse(reply);
            
            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(reply.getMentionedUserId())) {
                User mentionedUser = userRepository.findById(reply.getMentionedUserId()).orElse(null);
                if (mentionedUser != null) {
                    replyResponse.setMentionedUsername(mentionedUser.getUsername());
                }
            }
            
            replyResponses.add(replyResponse);
        }
        
        return replyResponses;
    }
    
    /**
     * Xóa một comment theo ID
     *
     * @param commentId ID của comment cần xóa
     * @throws ResourceNotFoundException Nếu comment không tồn tại
     */
    @Transactional
    @Override
    public void deleteComment(String commentId) {
        User currentUser = SecurityUtils.getCurrentUser();
        Comment comment = commentRepo.findById(commentId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_COMMENT_NOT_FOUND.message(),
                        ResultCode.MSG_COMMENT_NOT_FOUND.code()
                )
        );
        
        // Chỉ cho phép người dùng xóa comment của chính họ hoặc admin
        if (currentUser == null || (!currentUser.getId().equals(comment.getUser().getId()) 
                && !SecurityUtils.hasRole("ROLE_ADMIN"))) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_FORBIDDEN.message(),
                    ResultCode.MSG_FORBIDDEN.code()
            );
        }
        
        // Nếu là root comment, xóa cả replies
        if (comment.getParent() == null) {
            List<Comment> replies = commentRepo.findByParentIdOrderByCreatedDateAsc(commentId);
            commentRepo.deleteAll(replies);
        }
        
        commentRepo.delete(comment);
    }
    
    /**
     * Đếm số lượng root comments cho một bài viết
     *
     * @param blogPostId ID của bài viết
     * @return Số lượng root comments
     */
    @Override
    public long countRootCommentsByBlogPost(String blogPostId) {
        return commentRepo.countByBlogPostIdAndParentIsNull(blogPostId);
    }
    
    /**
     * Đếm số lượng comments cho một bài viết
     *
     * @param blogPostId ID của bài viết
     * @return Số lượng comments
     */
    @Override
    public long countCommentsByBlogPost(String blogPostId) {
        return commentRepo.countByBlogPostId(blogPostId);
    }
}
