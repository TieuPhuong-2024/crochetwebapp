package org.crochet.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VipSubscriptionRequest {
    private String paymentMethod;  // "paypal", "stripe", etc.
    private int months;            // Số tháng đăng ký
    private String transactionId;  // ID giao dịch từ cổng thanh toán
} 