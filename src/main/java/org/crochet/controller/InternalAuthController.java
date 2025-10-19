package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.crochet.payload.response.ResponseData;
import org.crochet.payload.response.UserInfo;
import org.crochet.service.JwtTokenService;
import org.crochet.service.UserService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Internal API controller for authentication operations
 * Used by blog service to validate JWT tokens
 */
@RestController
@RequestMapping("/api/v1/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    @Operation(summary = "Validate JWT token and return user info")
    @ApiResponse(responseCode = "200", description = "Token validated successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserInfo.class)))
    @ApiResponse(responseCode = "401", description = "Invalid token")
    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseData<UserInfo> validateToken(
            @Parameter(description = "JWT token in Bearer format")
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "Invalid token format");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtTokenService.validateToken(token)) {
                return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }

            // Extract username (which is user ID)
            String userId = jwtTokenService.extractUsername(token);

            // Get user details
            var user = userService.getById(userId);

            // Build UserInfo response
            UserInfo userInfo = UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .roles(user.getAuthorities().stream()
                            .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                            .collect(Collectors.toSet()))
                    .build();

            return ResponseUtil.success(userInfo);

        } catch (Exception e) {
            return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "Token validation failed: " + e.getMessage());
        }
    }
}
