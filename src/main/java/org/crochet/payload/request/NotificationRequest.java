package org.crochet.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crochet.model.Notification.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String message;
    
    private String link;
    
    @NotBlank
    private String receiverId;
    
    private String senderId;
    
    @NotNull
    private NotificationType notificationType;
} 