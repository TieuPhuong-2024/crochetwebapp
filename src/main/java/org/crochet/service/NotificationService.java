package org.crochet.service;

import org.crochet.payload.request.NotificationRequest;
import org.crochet.payload.response.NotificationResponse;
import org.crochet.payload.response.PaginationResponse;

public interface NotificationService {
    
    NotificationResponse createNotification(NotificationRequest notificationRequest);
    
    PaginationResponse<NotificationResponse> getUserNotifications(String userId, int page, int size);
    
    NotificationResponse markAsRead(String notificationId);
    
    long countUnreadNotifications(String userId);
    
    void markAllAsRead(String userId);
    
    void deleteNotification(String notificationId);
    
    void deleteAllUserNotifications(String userId);
} 