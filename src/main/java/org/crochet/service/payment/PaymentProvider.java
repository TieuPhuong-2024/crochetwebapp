package org.crochet.service.payment;

import java.math.BigDecimal;

public interface PaymentProvider {
    /**
     * Create a payment order.
     * @param amount The amount to pay
     * @param currency The currency (e.g., USD)
     * @param returnUrl The URL to redirect to after approval
     * @param cancelUrl The URL to redirect to if user cancels
     * @return The URL for user approval and the provider's order ID
     */
    PaymentOrderResponse createOrder(BigDecimal amount, String currency, String returnUrl, String cancelUrl);

    /**
     * Capture an approved payment order.
     * @param orderId The provider's order ID
     * @return Result of capture containing status and capture ID
     */
    PaymentCaptureResponse captureOrder(String orderId);

    String getProviderName();

    record PaymentOrderResponse(String orderId, String approveUrl) {}
    record PaymentCaptureResponse(boolean success, String captureId, String status, BigDecimal amount) {}
}
