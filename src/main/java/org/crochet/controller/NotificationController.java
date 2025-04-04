package org.crochet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.crochet.model.User;
import org.crochet.payload.request.NotificationRequest;
import org.crochet.payload.response.NotificationResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.security.CurrentUser;
import org.crochet.service.NotificationService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        var response = notificationService.createNotification(request);
        return ResponseUtil.success(response);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    public ResponseData<PaginationResponse<NotificationResponse>> getCurrentUserNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @CurrentUser User user) {
        var response = notificationService.getUserNotifications(user.getId(), page, size);
        return ResponseUtil.success(response);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.username")
    public ResponseData<PaginationResponse<NotificationResponse>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var response = notificationService.getUserNotifications(userId, page, size);
        return ResponseUtil.success(response);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/unread/count")
    public ResponseData<Long> getUnreadNotificationCount(@CurrentUser User user) {
        var countUnreadNotifications = notificationService.countUnreadNotifications(user.getId());
        return ResponseUtil.success(countUnreadNotifications);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}/read")
    public ResponseData<NotificationResponse> markNotificationAsRead(@PathVariable String id) {
        var response = notificationService.markAsRead(id);
        return ResponseUtil.success(response);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/read-all")
    public ResponseData<Void> markAllNotificationsAsRead(@CurrentUser User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseUtil.success();
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}")
    public ResponseData<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseUtil.success();
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/all")
    public ResponseData<Void> deleteAllCurrentUserNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.deleteAllUserNotifications(userDetails.getUsername());
        return ResponseUtil.success();
    }
} 