package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.Notification;
import org.crochet.model.User;
import org.crochet.payload.request.NotificationRequest;
import org.crochet.payload.response.NotificationResponse;
import org.crochet.repository.NotificationRepository;
import org.crochet.repository.UserRepository;
import org.crochet.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest notificationRequest) {
        User user = userRepository.findById(notificationRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + notificationRequest.getUserId()));

        Notification notification = Notification.builder()
                .title(notificationRequest.getTitle())
                .message(notificationRequest.getMessage())
                .link(notificationRequest.getLink())
                .isRead(false)
                .user(user)
                .notificationType(notificationRequest.getNotificationType())
                .build();

        notification = notificationRepository.save(notification);
        return convertToResponse(notification);
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationsPage = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return notificationsPage.map(this::convertToResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return convertToResponse(notification);
    }

    @Override
    public long countUnreadNotifications(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return notificationRepository.countUnreadNotifications(user);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Fetch all notifications for the user
        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged());
        
        notifications.forEach(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void deleteAllUserNotifications(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        notificationRepository.deleteAllByUser(user);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .notificationType(notification.getNotificationType())
                .build();
    }
} 