package org.crochet.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.event.CommentCreatedEvent;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.CommentMapper;
import org.crochet.model.BlogPost;
import org.crochet.model.Comment;
import org.crochet.model.FreePattern;
import org.crochet.model.Product;
import org.crochet.model.User;
import org.crochet.payload.request.CommentRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.repository.BlogPostRepository;
import org.crochet.repository.CommentRepository;
import org.crochet.repository.FreePatternRepository;
import org.crochet.repository.ProductRepository;
import org.crochet.repository.UserRepository;
import org.crochet.service.CommentService;
import org.crochet.util.ObjectUtils;
import org.crochet.util.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepo;
    private final BlogPostRepository blogPostRepo;
    private final ProductRepository productRepo;
    private final FreePatternRepository freePatternRepo;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CommentServiceImpl(CommentRepository commentRepo,
                              BlogPostRepository blogPostRepo,
                              ProductRepository productRepo,
                              FreePatternRepository freePatternRepo,
                              UserRepository userRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.commentRepo = commentRepo;
        this.blogPostRepo = blogPostRepo;
        this.productRepo = productRepo;
        this.freePatternRepo = freePatternRepo;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Tạo hoặc cập nhật một comment
     *
     * @param request chứa thông tin của comment
     * @return CommentResponse chứa thông tin của comment đã tạo/cập nhật
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

        // Kiểm tra xem có comment cha không
        Comment parent = null;
        if (ObjectUtils.hasText(request.getParentId())) {
            parent = commentRepo.findById(request.getParentId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_COMMENT_NOT_FOUND.message(),
                            ResultCode.MSG_COMMENT_NOT_FOUND.code()
                    )
            );

            // Đảm bảo chỉ cho phép độ sâu tối đa là 2 (root comment và replies)
            if (parent.getParent() != null) {
                // Nếu parent là reply, thì sử dụng parent của parent
                parent = parent.getParent();
            }
        }

        // Kiểm tra và lấy đối tượng tương ứng (blog post, product hoặc free pattern)
        BlogPost blog = null;
        Product product = null;
        FreePattern freePattern = null;

        // Đếm số lượng các ID không null để đảm bảo chỉ có 1 loại được chỉ định
        int idCount = 0;
        if (ObjectUtils.hasText(request.getBlogPostId())) {
            blog = blogPostRepo.findById(request.getBlogPostId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_BLOG_NOT_FOUND.message(),
                            ResultCode.MSG_BLOG_NOT_FOUND.code()
                    )
            );
            idCount++;
        }

        if (ObjectUtils.hasText(request.getProductId())) {
            product = productRepo.findById(request.getProductId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_PRODUCT_NOT_FOUND.message(),
                            ResultCode.MSG_PRODUCT_NOT_FOUND.code()
                    )
            );
            idCount++;
        }

        if (ObjectUtils.hasText(request.getFreePatternId())) {
            freePattern = freePatternRepo.findById(request.getFreePatternId()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                            ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()
                    )
            );
            idCount++;
        }

        // Đảm bảo chỉ có một loại ID được chỉ định
        if (idCount != 1) {
            throw new IllegalArgumentException("Phải chỉ định chính xác một trong ba loại ID: blogPostId, productId hoặc freePatternId");
        }

        // Tạo mới hoặc cập nhật comment
        Comment comment;
        String id = request.getId();
        if (!ObjectUtils.hasText(id)) {
            comment = Comment.builder()
                    .blogPost(blog)
                    .product(product)
                    .freePattern(freePattern)
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
        // Gửi sự kiện khi comment được tạo
        eventPublisher.publishEvent(new CommentCreatedEvent(comment));
        CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);

        // Thêm thông tin về người dùng được mention
        if (ObjectUtils.hasText(comment.getMentionedUserId())) {
            userRepository.findById(comment.getMentionedUserId())
                    .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
        }

        return response;
    }

    /**
     * Lấy danh sách root comments cho một bài viết với phân trang
     *
     * @param blogPostId ID của bài viết cần lấy comments
     * @param pageNo     Số trang (bắt đầu từ 0)
     * @param pageSize   Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách root comments và thông tin phân trang
     */
    @Transactional(readOnly = true)
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

            // Đếm số lượng replies cho comment này
            long replyCount = commentRepo.countByParentId(comment.getId());
            response.setReplyCount(replyCount);

            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                userRepository.findById(comment.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
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
     * @param pageNo     Số trang (bắt đầu từ 0)
     * @param pageSize   Số lượng comments mỗi trang
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
                userRepository.findById(comment.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
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
     * Lấy danh sách root comments cho một product với phân trang
     *
     * @param productId ID của product cần lấy comments
     * @param pageNo    Số trang (bắt đầu từ 0)
     * @param pageSize  Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách root comments và thông tin phân trang
     */
    @Transactional(readOnly = true)
    @Override
    public PaginationResponse<CommentResponse> getRootCommentsByProduct(String productId, int pageNo, int pageSize) {
        // Kiểm tra xem product có tồn tại không
        productRepo.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_PRODUCT_NOT_FOUND.message(),
                        ResultCode.MSG_PRODUCT_NOT_FOUND.code()
                )
        );

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comment> commentPage = commentRepo.findByProductIdAndParentIsNullOrderByCreatedDateDesc(productId, pageable);
        List<CommentResponse> rootComments = new ArrayList<>();

        for (Comment comment : commentPage.getContent()) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);

            // Đếm số lượng replies cho comment này
            long replyCount = commentRepo.countByParentId(comment.getId());
            response.setReplyCount(replyCount);

            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                userRepository.findById(comment.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
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
     * Lấy tất cả các comments cho một product (bao gồm cả root và replies)
     *
     * @param productId ID của product cần lấy comments
     * @param pageNo    Số trang (bắt đầu từ 0)
     * @param pageSize  Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách comments và thông tin phân trang
     */
    @Override
    public PaginationResponse<CommentResponse> getCommentsByProduct(String productId, int pageNo, int pageSize) {
        // Kiểm tra xem product có tồn tại không
        productRepo.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_PRODUCT_NOT_FOUND.message(),
                        ResultCode.MSG_PRODUCT_NOT_FOUND.code()
                )
        );

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comment> commentPage = commentRepo.findByProductIdOrderByCreatedDateDesc(productId, pageable);
        List<CommentResponse> comments = new ArrayList<>();

        for (Comment comment : commentPage.getContent()) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);

            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                userRepository.findById(comment.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
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
     * Lấy danh sách root comments cho một free pattern với phân trang
     *
     * @param freePatternId ID của free pattern cần lấy comments
     * @param pageNo        Số trang (bắt đầu từ 0)
     * @param pageSize      Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách root comments và thông tin phân trang
     */
    @Transactional(readOnly = true)
    @Override
    public PaginationResponse<CommentResponse> getRootCommentsByFreePattern(String freePatternId, int pageNo, int pageSize) {
        // Kiểm tra xem free pattern có tồn tại không
        freePatternRepo.findById(freePatternId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()
                )
        );

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comment> commentPage = commentRepo.findByFreePatternIdAndParentIsNullOrderByCreatedDateDesc(freePatternId, pageable);
        List<CommentResponse> rootComments = new ArrayList<>();

        for (Comment comment : commentPage.getContent()) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);

            // Đếm số lượng replies cho comment này
            long replyCount = commentRepo.countByParentId(comment.getId());
            response.setReplyCount(replyCount);

            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                userRepository.findById(comment.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
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
     * Lấy tất cả các comments cho một free pattern (bao gồm cả root và replies)
     *
     * @param freePatternId ID của free pattern cần lấy comments
     * @param pageNo        Số trang (bắt đầu từ 0)
     * @param pageSize      Số lượng comments mỗi trang
     * @return PaginationResponse chứa danh sách comments và thông tin phân trang
     */
    @Override
    public PaginationResponse<CommentResponse> getCommentsByFreePattern(String freePatternId, int pageNo, int pageSize) {
        // Kiểm tra xem free pattern có tồn tại không
        freePatternRepo.findById(freePatternId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()
                )
        );

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Comment> commentPage = commentRepo.findByFreePatternIdOrderByCreatedDateDesc(freePatternId, pageable);
        List<CommentResponse> comments = new ArrayList<>();

        for (Comment comment : commentPage.getContent()) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(comment);

            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                userRepository.findById(comment.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
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
    @Transactional(readOnly = true)
    @Override
    public List<CommentResponse> getRepliesByCommentId(String commentId) {
        // Kiểm tra xem comment có tồn tại không
        commentRepo.findById(commentId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_COMMENT_NOT_FOUND.message(),
                        ResultCode.MSG_COMMENT_NOT_FOUND.code()
                )
        );

        List<Comment> replies = commentRepo.findByParentIdOrderByCreatedDateAsc(commentId);
        List<CommentResponse> responses = new ArrayList<>();

        for (Comment reply : replies) {
            CommentResponse response = CommentMapper.INSTANCE.toResponse(reply);

            // Thêm thông tin về người dùng được mention
            if (ObjectUtils.hasText(reply.getMentionedUserId())) {
                userRepository.findById(reply.getMentionedUserId())
                        .ifPresent(mentionedUser -> response.setMentionedUsername(mentionedUser.getName()));
            }

            responses.add(response);
        }

        return responses;
    }

    /**
     * Xóa một comment
     *
     * @param commentId ID của comment cần xóa
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

    /**
     * Đếm số lượng root comments cho một product
     *
     * @param productId ID của product
     * @return Số lượng root comments
     */
    @Override
    public long countRootCommentsByProduct(String productId) {
        return commentRepo.countByProductIdAndParentIsNull(productId);
    }

    /**
     * Đếm số lượng comments cho một product
     *
     * @param productId ID của product
     * @return Số lượng comments
     */
    @Override
    public long countCommentsByProduct(String productId) {
        return commentRepo.countByProductId(productId);
    }

    /**
     * Đếm số lượng root comments cho một free pattern
     *
     * @param freePatternId ID của free pattern
     * @return Số lượng root comments
     */
    @Override
    public long countRootCommentsByFreePattern(String freePatternId) {
        return commentRepo.countByFreePatternIdAndParentIsNull(freePatternId);
    }

    /**
     * Đếm số lượng comments cho một free pattern
     *
     * @param freePatternId ID của free pattern
     * @return Số lượng comments
     */
    @Override
    public long countCommentsByFreePattern(String freePatternId) {
        return commentRepo.countByFreePatternId(freePatternId);
    }
}
