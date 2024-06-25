package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.CommentMapper;
import org.crochet.model.Comment;
import org.crochet.model.User;
import org.crochet.payload.request.CommentRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.repository.BlogPostRepository;
import org.crochet.repository.CommentRepository;
import org.crochet.repository.UserRepository;
import org.crochet.security.UserPrincipal;
import org.crochet.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static org.crochet.constant.MessageCodeConstant.MAP_CODE;
import static org.crochet.constant.MessageConstant.USER_NOT_FOUND_MESSAGE;

/**
 * CommentServiceImpl class
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    final CommentRepository commentRepo;
    final UserRepository userRepo;
    final BlogPostRepository blogPostRepo;

    /**
     * Creates a new comment or updates an existing one based on the provided {@link CommentRequest}.
     * If the request contains an ID, it updates the existing comment with the corresponding ID.
     * If the request does not contain an ID, it creates a new comment.
     *
     * @param principal The {@link UserPrincipal} containing information about the authenticated user.
     * @param request   The {@link CommentRequest} containing information for creating or updating the comment.
     * @return The {@link CommentResponse} containing information about the created or updated comment.
     * @throws ResourceNotFoundException If an existing comment is to be updated, and the specified ID is not found.
     */
    @Transactional
    @Override
    public CommentResponse createOrUpdate(UserPrincipal principal, CommentRequest request) {
        if (principal == null) {
            throw new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE, MAP_CODE.get(USER_NOT_FOUND_MESSAGE));
        }
        User user = userRepo.findById(principal.getId()).orElseThrow(
                () -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE, MAP_CODE.get(USER_NOT_FOUND_MESSAGE))
        );

        var blog = blogPostRepo.findById(request.getBlogPostId()).orElseThrow(
                () -> new ResourceNotFoundException("Blog post not found", MAP_CODE.get("Blog post not found"))
        );

        var id = request.getId();
        Comment comment;
        if (!StringUtils.hasText(id)) {
            comment = Comment.builder()
                    .blogPost(blog)
                    .user(user)
                    .build();
        } else {
            comment = commentRepo.findById(id).orElseThrow(
                    () -> new ResourceNotFoundException("Comment not found", MAP_CODE.get("Comment not found"))
            );
        }
        comment.setContent(request.getContent());
        comment.setCreatedDate(LocalDateTime.now());
        comment = commentRepo.save(comment);
        return CommentMapper.INSTANCE.toResponse(comment);
    }
}
