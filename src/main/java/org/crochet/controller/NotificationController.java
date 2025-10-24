package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Create notification")
    @ApiResponse(responseCode = "201", description = "Notification created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        var response = notificationService.createNotification(request);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get current user notifications")
    @ApiResponse(responseCode = "200", description = "Current user notifications retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginationResponse.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    public ResponseData<PaginationResponse<NotificationResponse>> getCurrentUserNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @CurrentUser User user) {
        var response = notificationService.getUserNotifications(user.getId(), page, size);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get user notifications by user ID")
    @ApiResponse(responseCode = "200", description = "User notifications retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginationResponse.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/user/{receiverId}")
    @PreAuthorize("hasRole('ADMIN') or #receiverId == authentication.principal.username")
    public ResponseData<PaginationResponse<NotificationResponse>> getUserNotifications(
            @PathVariable String receiverId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var response = notificationService.getUserNotifications(receiverId, page, size);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get unread notification count")
    @ApiResponse(responseCode = "200", description = "Unread notification count retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class)))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/unread/count")
    public ResponseData<Long> getUnreadNotificationCount(@CurrentUser User receiver) {
        var countUnreadNotifications = notificationService.countUnreadNotifications(receiver.getId());
        return ResponseUtil.success(countUnreadNotifications);
    }

    @Operation(summary = "Mark notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class)))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}/read")
    public ResponseData<NotificationResponse> markNotificationAsRead(@PathVariable String id) {
        var response = notificationService.markAsRead(id);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Mark all notifications as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/read-all")
    public ResponseData<Void> markAllNotificationsAsRead(@CurrentUser User receiver) {
        notificationService.markAllAsRead(receiver.getId());
        return ResponseUtil.success();
    }

    @Operation(summary = "Delete notification by ID")
    @ApiResponse(responseCode = "200", description = "Notification deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}")
    public ResponseData<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseUtil.success();
    }

    @Operation(summary = "Delete all current user notifications")
    @ApiResponse(responseCode = "200", description = "All notifications deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/all")
    public ResponseData<Void> deleteAllCurrentUserNotifications(@CurrentUser User receiver) {
        notificationService.deleteAllUserNotifications(receiver.getUsername());
        return ResponseUtil.success();
    }
}