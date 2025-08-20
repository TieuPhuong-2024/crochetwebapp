package org.crochet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crochet.model.Notification.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NatsNotificationEvent {
    private String eventId;
    private String title;
    private String message;
    private String link;
    private String receiverId;
    private String senderId;
    private NotificationType notificationType;
    private LocalDateTime timestamp;

    public static NatsNotificationEvent from(CommentCreatedEvent commentEvent) {
        return NatsNotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .title("Bình luận mới")
                .message("Có bình luận mới trong pattern của bạn")
                .link("/patterns/" + commentEvent.getComment().getFreePattern().getId())
                .receiverId(commentEvent.getComment().getFreePattern().getCreatedBy())
                .senderId(commentEvent.getComment().getUser().getId())
                .notificationType(NotificationType.COMMENT)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
