package org.crochet.event.handle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.event.CommentCreatedEvent;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.Comment;
import org.crochet.model.Notification.NotificationType;
import org.crochet.payload.request.NotificationRequest;
import org.crochet.repository.UserRepository;
import org.crochet.service.NatsPublisherService;
import org.crochet.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventListener {
    private final NatsPublisherService natsPublisherService;
    private final UserRepository userRepository;

    @Value("${nats.jetstream.enabled:true}")
    private boolean natsEnabled;

    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        Comment comment = event.getComment();

        log.info("Handling comment created event for comment ID: {}", comment.getId());

        if (natsEnabled) {
            // Publish notification requests directly to NATS
            publishCommentNotificationRequests(comment);
        } else {
            // Fallback to direct processing if NATS is disabled
            processCommentNotificationsDirectly(comment);
        }
    }

    private void publishCommentNotificationRequests(Comment comment) {
        try {
            // Publish notification for content creator
            if (comment.getFreePattern() != null) {
                var contentCreator = userRepository.findById(comment.getFreePattern().getCreatedBy())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ResultCode.MSG_USER_NOT_FOUND.message(),
                                ResultCode.MSG_USER_NOT_FOUND.code()));

                if (ObjectUtils.notEqual(comment.getUser().getId(), contentCreator.getId())) {
                    NotificationRequest request = NotificationRequest.builder()
                            .title("Bình luận mới")
                            .message("Có bình luận mới trong pattern của bạn")
                            .link("/free-patterns/" + comment.getFreePattern().getId())
                            .receiverId(contentCreator.getId())
                            .senderId(comment.getUser().getId())
                            .notificationType(NotificationType.COMMENT)
                            .build();

                    natsPublisherService.publishNotificationEvent("notifications.comment", request);
                    log.info("Content creator notification request published for user: {}", contentCreator.getId());
                }
            }

            // Publish notification for mentioned user
            if (ObjectUtils.hasText(comment.getMentionedUserId())) {
                var mentionedUser = userRepository.findById(comment.getMentionedUserId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ResultCode.MSG_USER_NOT_FOUND.message(),
                                ResultCode.MSG_USER_NOT_FOUND.code()));

                if (!ObjectUtils.equals(comment.getUser().getId(), mentionedUser.getId())) {
                    NotificationRequest request = NotificationRequest.builder()
                            .title("Bạn được nhắc đến trong bình luận")
                            .message(comment.getUser().getName() + " đã nhắc đến bạn trong một bình luận")
                            .link("/free-patterns/" + comment.getFreePattern().getId())
                            .receiverId(mentionedUser.getId())
                            .senderId(comment.getUser().getId())
                            .notificationType(NotificationType.COMMENT)
                            .build();

                    natsPublisherService.publishNotificationEvent("notifications.comment", request);
                    log.info("Mention notification request published for user: {}", mentionedUser.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish notification requests to NATS", e);
            // Fallback to direct processing if NATS fails
            processCommentNotificationsDirectly(comment);
        }
    }

    private void processCommentNotificationsDirectly(Comment comment) {
        log.info("Processing notifications directly (NATS fallback)");
        // TODO: Implement direct notification processing if needed
        log.warn("Direct notification processing not implemented - this is a fallback scenario");
    }
}