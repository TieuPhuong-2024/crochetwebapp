package org.crochet.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.crochet.enums.TargetType;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.BlogPost;
import org.crochet.model.FreePattern;
import org.crochet.model.Like;
import org.crochet.model.Pattern;
import org.crochet.model.Product;
import org.crochet.model.User;
import org.crochet.repository.BlogPostRepository;
import org.crochet.repository.FreePatternRepository;
import org.crochet.repository.LikeRepository;
import org.crochet.repository.PatternRepository;
import org.crochet.repository.ProductRepository;
import org.crochet.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PatternRepository patternRepository;
    private final BlogPostRepository blogPostRepository;
    private final ProductRepository productRepository;
    private final FreePatternRepository freePatternRepository;

    @Transactional
    public void increaseViewCount(String targetId, TargetType targetType) {
        String msg = String.format("The %s with id: %s is not found", targetType.name(), targetId);
        switch (targetType) {
            case PATTERN:
                Pattern pattern = patternRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                pattern.setViewCount(pattern.getViewCount() + 1);
                patternRepository.save(pattern);
                break;
            case BLOG:
                BlogPost blogPost = blogPostRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                blogPost.setViewCount(blogPost.getViewCount() + 1);
                blogPostRepository.save(blogPost);
                break;
            case PRODUCT:
                Product product = productRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                product.setViewCount(product.getViewCount() + 1);
                productRepository.save(product);
                break;
            case FREE_PATTERN:
                FreePattern freePattern = freePatternRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                freePattern.setViewCount(freePattern.getViewCount() + 1);
                freePatternRepository.save(freePattern);
                break;
            default:
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }

    @Transactional
    public boolean toggleLike(String userId, String targetId, TargetType targetType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isLiked = likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

        if (isLiked) {
            // Unlike
            Like like = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType).get();
            likeRepository.delete(like);
            updateLikeCount(targetId, targetType, -1);
            return false;
        } else {
            // Like
            Like like = Like.builder()
                    .user(user)
                    .targetId(targetId)
                    .targetType(targetType)
                    .createdDate(LocalDateTime.now())
                    .build();
            likeRepository.save(like);
            updateLikeCount(targetId, targetType, 1);
            return true;
        }
    }

    private void updateLikeCount(String targetId, TargetType targetType, int delta) {
        String msg = String.format("The %s with id: %s is not found", targetType.name(), targetId);
        switch (targetType) {
            case PATTERN:
                Pattern pattern = patternRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                pattern.setLikeCount(Math.max(0, pattern.getLikeCount() + delta));
                patternRepository.save(pattern);
                break;
            case BLOG:
                BlogPost blogPost = blogPostRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                blogPost.setLikeCount(Math.max(0, blogPost.getLikeCount() + delta));
                blogPostRepository.save(blogPost);
                break;
            case PRODUCT:
                Product product = productRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                product.setLikeCount(Math.max(0, product.getLikeCount() + delta));
                productRepository.save(product);
                break;
            case FREE_PATTERN:
                FreePattern freePattern = freePatternRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(msg));
                freePattern.setLikeCount(Math.max(0, freePattern.getLikeCount() + delta));
                freePatternRepository.save(freePattern);
                break;
            default:
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }
}
