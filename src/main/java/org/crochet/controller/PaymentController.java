package org.crochet.controller;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.PlanType;
import org.crochet.service.payment.PaymentProvider;
import org.crochet.service.payment.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentProvider.PaymentOrderResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        PaymentProvider.PaymentOrderResponse response = paymentService.createPayment(
                request.paymentMethod(),
                request.planType(),
                request.returnUrl(),
                request.cancelUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/capture")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> capturePayment(@RequestBody CapturePaymentRequest request) {
        boolean success = paymentService.capturePayment(request.orderId());
        if (success) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Payment captured successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Payment capture failed"));
        }
    }

    public record CreatePaymentRequest(String paymentMethod, PlanType planType, String returnUrl, String cancelUrl) {
    }

    public record CapturePaymentRequest(String orderId) {
    }
}
