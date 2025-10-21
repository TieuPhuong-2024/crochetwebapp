package org.crochet.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crochet.model.Notification.NotificationType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private String link;
    private boolean read;
    private Instant createdAt;
    private NotificationType notificationType;
    private String senderName;
    private String senderImageUrl;
} 