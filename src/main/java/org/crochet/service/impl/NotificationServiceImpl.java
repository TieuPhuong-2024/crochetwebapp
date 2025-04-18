package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.NotificationMapper;
import org.crochet.model.Notification;
import org.crochet.model.User;
import org.crochet.payload.request.NotificationRequest;
import org.crochet.payload.response.NotificationResponse;
import org.crochet.payload.response.PaginationResponse;
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
    private final NotificationMapper notificationMapper = NotificationMapper.INSTANCE;

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest notificationRequest) {
        User receiver = userRepository.findById(notificationRequest.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + notificationRequest.getReceiverId()));

        User sender = null;
        if (notificationRequest.getSenderId() != null) {
            sender = userRepository.findById(notificationRequest.getSenderId())
                    .orElse(null);
        }

        Notification notification = Notification.builder()
                .title(notificationRequest.getTitle())
                .message(notificationRequest.getMessage())
                .link(notificationRequest.getLink())
                .isRead(false)
                .receiver(receiver)
                .sender(sender)
                .notificationType(notificationRequest.getNotificationType())
                .build();

        notification = notificationRepository.save(notification);
        return notificationMapper.toResponse(notification);
    }

    @Transactional(readOnly = true)
    @Override
    public PaginationResponse<NotificationResponse> getUserNotifications(String receiverId, int page, int size) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + receiverId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationsPage = notificationRepository.findByReceiverOrderByCreatedAtDesc(receiver, pageable);
        Page<NotificationResponse> notifications = notificationsPage.map(notificationMapper::toResponse);
        return PaginationResponse.<NotificationResponse>builder()
                .contents(notifications.getContent())
                .pageNo(notificationsPage.getNumber())
                .pageSize(notificationsPage.getSize())
                .totalPages(notificationsPage.getTotalPages())
                .totalElements(notificationsPage.getTotalElements())
                .last(notificationsPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return notificationMapper.toResponse(notification);
    }

    @Override
    public long countUnreadNotifications(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return notificationRepository.countUnreadNotifications(user);
    }

    @Override
    @Transactional
    public void markAllAsRead(String receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + receiverId));

        // Fetch all notifications for the receiver
        Page<Notification> notifications = notificationRepository.findByReceiverOrderByCreatedAtDesc(receiver, Pageable.unpaged());

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
    public void deleteAllUserNotifications(String receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + receiverId));

        notificationRepository.deleteAllByReceiver(receiver);
    }
} 