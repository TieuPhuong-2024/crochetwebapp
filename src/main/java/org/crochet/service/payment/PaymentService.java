package org.crochet.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.crochet.exception.BadRequestException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.enums.CurrencyCode;
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

    private BigDecimal getPrice(PlanType planType, CurrencyCode currency) {
        String baseKey = planType == PlanType.MONTHLY ? "PAYMENT_MONTHLY_PRICE" : "PAYMENT_YEARLY_PRICE";
        String key = currency == CurrencyCode.USD ? baseKey : baseKey + "_" + currency.getValue();
        String defaultValue = currency == CurrencyCode.USD
                ? (planType == PlanType.MONTHLY ? "9.99" : "99.99")
                : null;

        Settings setting = settingsUtil.getSettingsMap().get(key);
        String value = setting != null ? setting.getValue() : defaultValue;
        if (value == null) {
            throw new BadRequestException("Subscription price is not configured for " + currency.getValue());
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.error("Invalid price format in settings for key {}: {}", key, value);
            if (defaultValue != null) {
                return new BigDecimal(defaultValue);
            }
            throw new BadRequestException("Invalid subscription price for " + currency.getValue());
        }
    }
    
    public Map<String, BigDecimal> getPrices() {
        return Map.of(
            "MONTHLY", getPrice(PlanType.MONTHLY, CurrencyCode.USD),
            "YEARLY", getPrice(PlanType.YEARLY, CurrencyCode.USD)
        );
    }

    private PlanType resolvePlanType(PaymentTransaction transaction) {
        if (transaction.getPlanType() != null) {
            return transaction.getPlanType();
        }

        // Compatibility path for pending transactions created before plan_type existed.
        BigDecimal monthlyPrice = getPrice(PlanType.MONTHLY, transaction.getCurrency());
        BigDecimal yearlyPrice = getPrice(PlanType.YEARLY, transaction.getCurrency());
        if (transaction.getAmount().compareTo(monthlyPrice) == 0) {
            return PlanType.MONTHLY;
        }
        if (transaction.getAmount().compareTo(yearlyPrice) == 0) {
            return PlanType.YEARLY;
        }
        throw new BadRequestException("Cannot determine plan for legacy payment; create a new payment order");
    }

    @Transactional
    public PaymentProvider.PaymentOrderResponse createPayment(String paymentMethod, PlanType planType, String returnUrl,
            String cancelUrl) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BadRequestException("User not authenticated");
        }

        // Check if user is already PREMIUM
        Optional<Subscription> activeSub = subscriptionRepository.findByUserIdAndStatus(currentUser.getId(),
                SubscriptionStatus.ACTIVE);
        if (activeSub.isPresent() && activeSub.get().getEndDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("You already have an active premium subscription.");
        }

        PaymentProvider provider = paymentProviders.get(paymentMethod.toUpperCase());
        if (provider == null) {
            throw new BadRequestException("Payment method not supported");
        }

        CurrencyCode currency = provider.getCurrency();
        BigDecimal amount = getPrice(planType, currency);

        // Call provider to create order
        PaymentProvider.PaymentOrderResponse orderResponse = provider.createOrder(amount, currency, returnUrl, cancelUrl);

        // Save pending transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(currentUser)
                .paymentMethod(paymentMethod.toUpperCase())
                .providerOrderId(orderResponse.orderId())
                .amount(amount)
                .currency(currency)
                .planType(planType)
                .status(PaymentStatus.PENDING)
                .build();
        paymentTransactionRepository.save(transaction);

        return orderResponse;
    }

    @Transactional
    public boolean capturePayment(String orderId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByProviderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is already processed");
        }

        PlanType planType = resolvePlanType(transaction);
        PaymentProvider provider = paymentProviders.get(transaction.getPaymentMethod());
        PaymentProvider.PaymentCaptureResponse captureResponse = provider.captureOrder(orderId);

        if (captureResponse.success()) {
            // Verify amount
            if (captureResponse.amount().compareTo(transaction.getAmount()) != 0) {
                log.warn("Captured amount {} does not match requested amount {}", captureResponse.amount(),
                        transaction.getAmount());
                transaction.setStatus(PaymentStatus.FAILED);
                paymentTransactionRepository.save(transaction);
                throw new BadRequestException("Payment amount mismatch");
            }

            // Update transaction
            transaction.setStatus(PaymentStatus.COMPLETED);
            transaction.setProviderCaptureId(captureResponse.captureId());
            paymentTransactionRepository.save(transaction);

            // Create/Update subscription
            User user = transaction.getUser();
            transaction.setPlanType(planType);
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
