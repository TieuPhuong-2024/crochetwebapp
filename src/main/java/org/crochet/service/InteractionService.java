package org.crochet.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.crochet.enums.TargetType;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.Like;
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
        int updated = switch (targetType) {
            case PATTERN -> patternRepository.incrementViewCount(targetId);
            case BLOG -> blogPostRepository.incrementViewCount(targetId);
            case PRODUCT -> productRepository.incrementViewCount(targetId);
            case FREE_PATTERN -> freePatternRepository.incrementViewCount(targetId);
        };

        if (updated == 0) {
            String msg = String.format("The %s with id: %s is not found", targetType.name(), targetId);
            throw new ResourceNotFoundException(msg);
        }
    }

    @Transactional
    public boolean toggleLike(String userId, String targetId, TargetType targetType) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        boolean isLiked = likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

        if (isLiked) {
            // Unlike - delete and decrement count
            likeRepository.deleteByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
            decrementLikeCount(targetId, targetType);
            return false;
        } else {
            // Like - create new like and increment count
            User user = userRepository.getReferenceById(userId);
            Like like = Like.builder()
                    .user(user)
                    .targetId(targetId)
                    .targetType(targetType)
                    .createdDate(LocalDateTime.now())
                    .build();
            likeRepository.save(like);
            incrementLikeCount(targetId, targetType);
            return true;
        }
    }

    private void incrementLikeCount(String targetId, TargetType targetType) {
        int updated = switch (targetType) {
            case PATTERN -> patternRepository.incrementLikeCount(targetId);
            case BLOG -> blogPostRepository.incrementLikeCount(targetId);
            case PRODUCT -> productRepository.incrementLikeCount(targetId);
            case FREE_PATTERN -> freePatternRepository.incrementLikeCount(targetId);
        };

        if (updated == 0) {
            String msg = String.format("The %s with id: %s is not found", targetType.name(), targetId);
            throw new ResourceNotFoundException(msg);
        }
    }

    private void decrementLikeCount(String targetId, TargetType targetType) {
        int updated = switch (targetType) {
            case PATTERN -> patternRepository.decrementLikeCount(targetId);
            case BLOG -> blogPostRepository.decrementLikeCount(targetId);
            case PRODUCT -> productRepository.decrementLikeCount(targetId);
            case FREE_PATTERN -> freePatternRepository.decrementLikeCount(targetId);
        };

        if (updated == 0) {
            String msg = String.format("The %s with id: %s is not found", targetType.name(), targetId);
            throw new ResourceNotFoundException(msg);
        }
    }
}
