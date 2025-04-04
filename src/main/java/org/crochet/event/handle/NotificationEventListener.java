package org.crochet.event.handle;

import org.crochet.enums.ResultCode;
import org.crochet.event.CommentCreatedEvent;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.Comment;
import org.crochet.model.Notification.NotificationType;
import org.crochet.payload.request.NotificationRequest;
import org.crochet.repository.UserRepository;
import org.crochet.service.NotificationService;
import org.crochet.util.CommentUtils;
import org.crochet.util.ObjectUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        Comment comment = event.getComment();
        
        log.info("Handling comment created event for comment ID: {}", comment.getId());
        
        var recipient = userRepository.findById(comment.getFreePattern().getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_USER_NOT_FOUND.message(),
                        ResultCode.MSG_USER_NOT_FOUND.code()));
        
        log.info("Found recipient user with ID: {}", recipient.getId());

        // Không gửi thông báo cho người tạo bài viết
        if (ObjectUtils.equals(comment.getUser().getId(), recipient.getId())) {
            log.info("Skipping notification as commenter is the post author");
            return;
        }

        log.info("Creating notification for user: {}", recipient.getId());
        
        NotificationRequest notification = NotificationRequest.builder()
                .title("Bình luận mới")
                .message(CommentUtils.getMessage(comment))
                .link(CommentUtils.getLink(comment))
                .userId(recipient.getId())
                .notificationType(NotificationType.COMMENT)
                .build();

        notificationService.createNotification(notification);
        log.info("Notification created successfully");
    }
}
