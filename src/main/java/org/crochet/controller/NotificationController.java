package org.crochet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.crochet.payload.request.NotificationRequest;
import org.crochet.payload.response.NotificationResponse;
import org.crochet.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createNotification(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<NotificationResponse>> getCurrentUserNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userDetails.getUsername(), page, size));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.username")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, size));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(userDetails.getUsername()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markNotificationAsRead(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllCurrentUserNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.deleteAllUserNotifications(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
} 