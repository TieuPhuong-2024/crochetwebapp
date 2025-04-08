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
import org.crochet.service.NotificationService;
import org.crochet.util.CommentUtils;
import org.crochet.util.ObjectUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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

        // Xử lý thông báo cho người tạo nội dung (đã có sẵn)
        if (comment.getFreePattern() != null) {
            var contentCreator = userRepository.findById(comment.getFreePattern().getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ResultCode.MSG_USER_NOT_FOUND.message(),
                            ResultCode.MSG_USER_NOT_FOUND.code()));

            log.info("Found content creator with ID: {}", contentCreator.getId());

            // Không gửi thông báo cho người tạo nếu tự họ comment
            if (ObjectUtils.notEqual(comment.getUser().getId(), contentCreator.getId())) {
                log.info("Creating notification for content creator: {}", contentCreator.getId());

                NotificationRequest notification = NotificationRequest.builder()
                        .title("Bình luận mới")
                        .message(CommentUtils.getMessage(comment))
                        .link(CommentUtils.getLink(comment))
                        .userId(contentCreator.getId())
                        .senderId(comment.getUser().getId())
                        .notificationType(NotificationType.COMMENT)
                        .build();

                notificationService.createNotification(notification);
                log.info("Content creator notification created successfully");
            }
        }

        // Thêm mới: Xử lý thông báo cho người được mention
        if (ObjectUtils.hasText(comment.getMentionedUserId())) {
            var mentionedUser = userRepository.findById(comment.getMentionedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ResultCode.MSG_USER_NOT_FOUND.message(),
                            ResultCode.MSG_USER_NOT_FOUND.code()));

            log.info("Found mentioned user with ID: {}", mentionedUser.getId());

            // Không gửi thông báo nếu người được mention chính là người comment
            if (!ObjectUtils.equals(comment.getUser().getId(), mentionedUser.getId())) {
                log.info("Creating notification for mentioned user: {}", mentionedUser.getId());

                NotificationRequest notification = NotificationRequest.builder()
                        .title("Bạn được nhắc đến trong bình luận")
                        .message(comment.getUser().getName() + " đã nhắc đến bạn trong một bình luận")
                        .link(CommentUtils.getLink(comment))
                        .userId(mentionedUser.getId())
                        .senderId(comment.getUser().getId())
                        .notificationType(NotificationType.COMMENT)
                        .build();

                notificationService.createNotification(notification);
                log.info("Mention notification created successfully");
            }
        }
    }
}