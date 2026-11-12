package org.crochet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.crochet.enums.TargetType;
import org.crochet.model.User;
import org.crochet.payload.response.ResponseData;
import org.crochet.security.CurrentUser;
import org.crochet.service.InteractionService;
import org.crochet.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @Operation(summary = "Increase view count")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/view/{type}/{id}")
    public ResponseData<Void> increaseView(
            @PathVariable("type") TargetType type,
            @PathVariable("id") String id) {
        interactionService.increaseViewCount(id, type);
        return ResponseUtil.success(HttpStatus.OK, "View count increased");
    }

    @Operation(summary = "Toggle like on an item")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/like/toggle")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseData<Boolean> toggleLike(
            @CurrentUser User currentUser,
            @RequestParam("targetId") String targetId,
            @RequestParam("targetType") TargetType targetType) {
        boolean isLiked = interactionService.toggleLike(currentUser.getId(), targetId, targetType);
        return ResponseUtil.success(isLiked, isLiked ? "Liked" : "Unliked");
    }
}
