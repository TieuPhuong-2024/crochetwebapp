package org.crochet.service.payment;

import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.AmountWithBreakdown;
import com.paypal.sdk.models.OrdersCapture;
import com.paypal.sdk.models.CaptureOrderInput;
import com.paypal.sdk.models.CheckoutPaymentIntent;
import com.paypal.sdk.models.CreateOrderInput;
import com.paypal.sdk.models.LinkDescription;
import com.paypal.sdk.models.Order;
import com.paypal.sdk.models.OrderApplicationContext;
import com.paypal.sdk.models.OrderApplicationContextUserAction;
import com.paypal.sdk.models.OrderRequest;
import com.paypal.sdk.models.PurchaseUnit;
import com.paypal.sdk.models.PurchaseUnitRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalPaymentProvider implements PaymentProvider {

    private final PaypalServerSdkClient payPalHttpClient;

    @Override
    public PaymentOrderResponse createOrder(BigDecimal amount, String currency, String returnUrl, String cancelUrl) {
        OrderApplicationContext applicationContext = new OrderApplicationContext.Builder()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .userAction(OrderApplicationContextUserAction.PAY_NOW)
                .build();

        List<PurchaseUnitRequest> purchaseUnitRequests = new ArrayList<>();
        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest.Builder()
                .amount(new AmountWithBreakdown.Builder(currency, amount.toString()).build())
                .build();
        purchaseUnitRequests.add(purchaseUnitRequest);

        OrderRequest orderRequest = new OrderRequest.Builder(CheckoutPaymentIntent.CAPTURE, purchaseUnitRequests)
                .applicationContext(applicationContext)
                .build();

        CreateOrderInput request = new CreateOrderInput.Builder()
                .body(orderRequest)
                .build();

        try {
            OrdersController ordersController = payPalHttpClient.getOrdersController();
            ApiResponse<Order> response = ordersController.createOrder(request);
            Order order = response.getResult();

            String approveUrl = order.getLinks().stream()
                    .filter(link -> "approve".equals(link.getRel()))
                    .map(LinkDescription::getHref)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Approve URL not found in PayPal response"));

            return new PaymentOrderResponse(order.getId(), approveUrl);
        } catch (Exception e) {
            log.error("Error creating PayPal order", e);
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage());
        }
    }

    @Override
    public PaymentCaptureResponse captureOrder(String orderId) {
        CaptureOrderInput request = new CaptureOrderInput.Builder()
                .id(orderId)
                .build();

        try {
            OrdersController ordersController = payPalHttpClient.getOrdersController();
            ApiResponse<Order> response = ordersController.captureOrder(request);
            Order order = response.getResult();

            if ("COMPLETED".equals(order.getStatus().value())) {
                PurchaseUnit purchaseUnit = order.getPurchaseUnits().get(0);
                OrdersCapture capture = purchaseUnit.getPayments().getCaptures().get(0);
                BigDecimal amount = new BigDecimal(capture.getAmount().getValue());
                return new PaymentCaptureResponse(true, capture.getId(), capture.getStatus().value(), amount);
            } else {
                return new PaymentCaptureResponse(false, null, order.getStatus().value(), BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.error("Error capturing PayPal order {}", orderId, e);
            throw new RuntimeException("Failed to capture PayPal order: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "PAYPAL";
    }
}
