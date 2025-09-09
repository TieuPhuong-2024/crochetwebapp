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

        // Xử lý thông báo cho người tạo nội dung (đã có sẵn)
        if (comment.getFreePattern() != null) {
            var contentCreator = userRepository.findById(comment.getFreePattern().getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ResultCode.MSG_USER_NOT_FOUND.message(),
                            ResultCode.MSG_USER_NOT_FOUND.code()));

            // Không gửi thông báo cho người tạo nếu tự họ comment
            if (ObjectUtils.notEqual(comment.getUser().getId(), contentCreator.getId())) {

                NotificationRequest notification = NotificationRequest.builder()
                        .title("Bình luận mới")
                        .message(CommentUtils.getMessage(comment))
                        .link(CommentUtils.getLink(comment))
                        .receiverId(contentCreator.getId())
                        .senderId(comment.getUser().getId())
                        .notificationType(NotificationType.COMMENT)
                        .build();

                notificationService.createNotification(notification);
            }
        }

        // Thêm mới: Xử lý thông báo cho người được mention
        if (ObjectUtils.hasText(comment.getMentionedUserId())) {
            var mentionedUser = userRepository.findById(comment.getMentionedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ResultCode.MSG_USER_NOT_FOUND.message(),
                            ResultCode.MSG_USER_NOT_FOUND.code()));

            // Không gửi thông báo nếu người được mention chính là người comment
            if (!ObjectUtils.equals(comment.getUser().getId(), mentionedUser.getId())) {

                NotificationRequest notification = NotificationRequest.builder()
                        .title("Bạn được nhắc đến trong bình luận")
                        .message(comment.getUser().getName() + " đã nhắc đến bạn trong một bình luận")
                        .link(CommentUtils.getLink(comment))
                        .receiverId(mentionedUser.getId())
                        .senderId(comment.getUser().getId())
                        .notificationType(NotificationType.COMMENT)
                        .build();

                notificationService.createNotification(notification);
            }
        }
    }
}