package org.crochet.controller;

import lombok.RequiredArgsConstructor;
import org.crochet.payload.request.VipSubscriptionRequest;
import org.crochet.payload.response.ApiResponse;
import org.crochet.service.VipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {
    
    private final VipService vipService;
    
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Boolean>> subscribeVip(@RequestBody VipSubscriptionRequest request) {
        boolean result = vipService.subscribeVip(request);
        return ResponseEntity.ok(new ApiResponse<>(result, "VIP subscription processed successfully"));
    }
    
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkVipStatus() {
        boolean isVip = vipService.isCurrentUserVip();
        return ResponseEntity.ok(new ApiResponse<>(isVip, "VIP status checked successfully"));
    }
} 