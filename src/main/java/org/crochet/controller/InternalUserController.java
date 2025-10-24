package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.crochet.payload.response.ResponseData;
import org.crochet.service.UserService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal API controller for user operations
 * Used by blog service to get user information
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @Operation(summary = "Get user info by user ID")
    @ApiResponse(responseCode = "200", description = "User info retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = org.crochet.payload.response.UserResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseData<org.crochet.payload.response.UserResponse> getUserInfo(
            @Parameter(description = "User ID") @PathVariable("userId") String userId) {

        try {
            var userResponse = userService.getDetail(userId);
            return ResponseUtil.success(userResponse);
        } catch (Exception e) {
            return ResponseUtil.error(HttpStatus.NOT_FOUND, "User not found: " + e.getMessage());
        }
    }

    @Operation(summary = "Get batch user info")
    @ApiResponse(responseCode = "200", description = "Batch user info retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @GetMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public ResponseData<List<org.crochet.payload.response.UserResponse>> getBatchUserInfo(
            @Parameter(description = "Comma-separated list of user IDs")
            @RequestParam("userIds") String userIds) {

        try {
            if (userIds == null || userIds.trim().isEmpty()) {
                return ResponseUtil.error(HttpStatus.BAD_REQUEST, "userIds parameter is required");
            }

            String[] ids = userIds.split(",");
            List<org.crochet.payload.response.UserResponse> userResponses = java.util.Arrays.stream(ids)
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(userService::getDetail)
                    .toList();

            return ResponseUtil.success(userResponses);

        } catch (Exception e) {
            return ResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get batch user info: " + e.getMessage());
        }
    }
}
