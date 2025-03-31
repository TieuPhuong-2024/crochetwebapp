package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.crochet.payload.response.FreePatternResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.payload.response.ResponseData;
import org.crochet.service.HeartService;
import org.crochet.util.ResponseUtil;
import org.crochet.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hearts")
@Tag(name = "Heart API", description = "API for managing hearts/likes")
@RequiredArgsConstructor
public class HeartController {
    private final HeartService heartService;

    @Operation(summary = "Toggle heart (like/unlike) for a free pattern")
    @ApiResponse(responseCode = "200", description = "Heart toggled successfully")
    @PostMapping("/{freePatternId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<Map<String, Boolean>> toggleHeart(
            @Parameter(description = "ID of the free pattern")
            @PathVariable String freePatternId) {
        boolean isHearted = heartService.toggleHeart(freePatternId);
        return ResponseUtil.success(Map.of("isHearted", isHearted));
    }

    @Operation(summary = "Check if a free pattern is hearted by the current user")
    @ApiResponse(responseCode = "200", description = "Heart status retrieved successfully")
    @GetMapping("/check/{freePatternId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<Map<String, Boolean>> checkHeart(
            @Parameter(description = "ID of the free pattern")
            @PathVariable String freePatternId) {
        var currentUser = SecurityUtils.getCurrentUser();
        boolean isHearted = heartService.isHearted(freePatternId, currentUser.getId());
        return ResponseUtil.success(Map.of("isHearted", isHearted));
    }

    @Operation(summary = "Get count of hearts for a free pattern")
    @ApiResponse(responseCode = "200", description = "Heart count retrieved successfully")
    @GetMapping("/count/{freePatternId}")
    public ResponseData<Map<String, Integer>> getHeartCount(
            @Parameter(description = "ID of the free pattern")
            @PathVariable String freePatternId) {
        int heartCount = heartService.countHearts(freePatternId);
        return ResponseUtil.success(Map.of("heartCount", heartCount));
    }

    @Operation(summary = "Get free patterns hearted by a user")
    @ApiResponse(responseCode = "200", description = "Hearted free patterns retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PaginationResponse.class)))
    @GetMapping("/user/{userId}")
    public ResponseData<PaginationResponse<FreePatternResponse>> getHeartedFreePatterns(
            @Parameter(description = "ID of the user")
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginationResponse<FreePatternResponse> response = heartService.getHeartedFreePatterns(userId, pageNo, pageSize);
        return ResponseUtil.success(response);
    }

    @Operation(summary = "Get IDs of free patterns hearted by the current user")
    @ApiResponse(responseCode = "200", description = "Hearted free pattern IDs retrieved successfully")
    @GetMapping("/me/ids")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseData<List<String>> getMyHeartedFreePatternIds() {
        var currentUser = SecurityUtils.getCurrentUser();
        List<String> freePatternIds = heartService.getHeartedFreePatternIds(currentUser.getId());
        return ResponseUtil.success(freePatternIds);
    }
} 