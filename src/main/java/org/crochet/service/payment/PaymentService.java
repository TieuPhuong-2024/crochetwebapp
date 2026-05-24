package org.crochet.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.PaymentStatus;
import org.crochet.enums.PlanType;
import org.crochet.enums.RoleType;
import org.crochet.enums.SubscriptionStatus;
import org.crochet.model.PaymentTransaction;
import org.crochet.model.Subscription;
import org.crochet.model.User;
import org.crochet.model.Settings;
import org.crochet.util.SettingsUtil;
import org.crochet.repository.PaymentTransactionRepository;
import org.crochet.repository.SubscriptionRepository;
import org.crochet.repository.UserRepository;
import org.crochet.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final Map<String, PaymentProvider> paymentProviders;
    private final SettingsUtil settingsUtil;

    public PaymentService(PaymentTransactionRepository paymentTransactionRepository,
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            SettingsUtil settingsUtil,
            List<PaymentProvider> providerList) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.settingsUtil = settingsUtil;
        this.paymentProviders = providerList.stream()
                .collect(Collectors.toMap(PaymentProvider::getProviderName, Function.identity()));
    }

    private BigDecimal getPrice(PlanType planType) {
        String key = planType == PlanType.MONTHLY ? "PAYMENT_MONTHLY_PRICE" : "PAYMENT_YEARLY_PRICE";
        String defaultValue = planType == PlanType.MONTHLY ? "9.99" : "99.99";

        Settings setting = settingsUtil.getSettingsMap().get(key);
        String value = setting != null ? setting.getValue() : defaultValue;

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.error("Invalid price format in settings for key {}: {}", key, value);
            return new BigDecimal(defaultValue);
        }
    }
    
    public Map<String, BigDecimal> getPrices() {
        return Map.of(
            "MONTHLY", getPrice(PlanType.MONTHLY),
            "YEARLY", getPrice(PlanType.YEARLY)
        );
    }

    @Transactional
    public PaymentProvider.PaymentOrderResponse createPayment(String paymentMethod, PlanType planType, String returnUrl,
            String cancelUrl) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Check if user is already PREMIUM
        Optional<Subscription> activeSub = subscriptionRepository.findByUserIdAndStatus(currentUser.getId(),
                SubscriptionStatus.ACTIVE);
        if (activeSub.isPresent() && activeSub.get().getEndDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("You already have an active premium subscription.");
        }

        PaymentProvider provider = paymentProviders.get(paymentMethod.toUpperCase());
        if (provider == null) {
            throw new RuntimeException("Payment method not supported");
        }

        BigDecimal amount = getPrice(planType);

        // Call provider to create order
        PaymentProvider.PaymentOrderResponse orderResponse = provider.createOrder(amount, "USD", returnUrl, cancelUrl);

        // Save pending transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(currentUser)
                .paymentMethod(paymentMethod.toUpperCase())
                .providerOrderId(orderResponse.orderId())
                .amount(amount)
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .build();
        paymentTransactionRepository.save(transaction);

        return orderResponse;
    }

    @Transactional
    public boolean capturePayment(String orderId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByProviderOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is already processed");
        }

        PaymentProvider provider = paymentProviders.get(transaction.getPaymentMethod());
        PaymentProvider.PaymentCaptureResponse captureResponse = provider.captureOrder(orderId);

        if (captureResponse.success()) {
            // Verify amount
            if (captureResponse.amount().compareTo(transaction.getAmount()) != 0) {
                log.warn("Captured amount {} does not match requested amount {}", captureResponse.amount(),
                        transaction.getAmount());
                transaction.setStatus(PaymentStatus.FAILED);
                paymentTransactionRepository.save(transaction);
                throw new RuntimeException("Payment amount mismatch");
            }

            // Update transaction
            transaction.setStatus(PaymentStatus.COMPLETED);
            transaction.setProviderCaptureId(captureResponse.captureId());
            paymentTransactionRepository.save(transaction);

            // Create/Update subscription
            User user = transaction.getUser();
            BigDecimal monthlyPrice = getPrice(PlanType.MONTHLY);
            BigDecimal yearlyPrice = getPrice(PlanType.YEARLY);

            BigDecimal diffMonthly = transaction.getAmount().subtract(monthlyPrice).abs();
            BigDecimal diffYearly = transaction.getAmount().subtract(yearlyPrice).abs();

            PlanType planType = diffMonthly.compareTo(diffYearly) < 0 ? PlanType.MONTHLY : PlanType.YEARLY;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = planType == PlanType.MONTHLY ? now.plusMonths(1) : now.plusYears(1);

            Subscription subscription = Subscription.builder()
                    .user(user)
                    .planType(planType)
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(now)
                    .endDate(endDate)
                    .build();
            subscription = subscriptionRepository.save(subscription);

            // Link subscription to transaction
            transaction.setSubscription(subscription);
            paymentTransactionRepository.save(transaction);

            // Update user role to PREMIUM_USER
            user.setRole(RoleType.PREMIUM_USER);
            userRepository.save(user);

            return true;
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            return false;
        }
    }
}
