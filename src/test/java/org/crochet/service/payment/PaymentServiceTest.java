package org.crochet.service.payment;

import org.crochet.enums.CurrencyCode;
import org.crochet.enums.PaymentStatus;
import org.crochet.enums.PlanType;
import org.crochet.model.PaymentTransaction;
import org.crochet.model.Settings;
import org.crochet.model.Subscription;
import org.crochet.model.User;
import org.crochet.repository.PaymentTransactionRepository;
import org.crochet.repository.SubscriptionRepository;
import org.crochet.repository.UserRepository;
import org.crochet.util.SettingsUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SettingsUtil settingsUtil;
    @Mock
    private PaymentProvider paymentProvider;

    private PaymentService paymentService;
    private User user;

    @BeforeEach
    void setUp() {
        when(paymentProvider.getProviderName()).thenReturn("PAYPAL");
        when(settingsUtil.getSettingsMap()).thenReturn(Map.of(
                "PAYMENT_MONTHLY_PRICE", Settings.builder().key("PAYMENT_MONTHLY_PRICE").value("9.99").build(),
                "PAYMENT_YEARLY_PRICE", Settings.builder().key("PAYMENT_YEARLY_PRICE").value("99.99").build()));

        paymentService = new PaymentService(
                paymentTransactionRepository,
                subscriptionRepository,
                userRepository,
                settingsUtil,
                List.of(paymentProvider));

        user = User.builder().id("user-1").name("Buyer").email("buyer@example.com").password("secret").build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user, null, "ROLE_USER"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPaymentUsesProviderCurrencyAndStoresPlanType() {
        when(paymentProvider.getCurrency()).thenReturn(CurrencyCode.USD);
        when(subscriptionRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());
        when(paymentProvider.createOrder(any(), any(), any(), any()))
                .thenReturn(new PaymentProvider.PaymentOrderResponse("order-1", "https://paypal.test/order-1"));

        paymentService.createPayment("paypal", PlanType.MONTHLY, "https://return.test", "https://cancel.test");

        verify(paymentProvider).createOrder(
                new BigDecimal("9.99"), CurrencyCode.USD, "https://return.test", "https://cancel.test");
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        PaymentTransaction transaction = transactionCaptor.getValue();
        assertThat(transaction.getCurrency()).isEqualTo(CurrencyCode.USD);
        assertThat(transaction.getPlanType()).isEqualTo(PlanType.MONTHLY);
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void capturePaymentResolvesPlanTypeForLegacyTransaction() {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .paymentMethod("PAYPAL")
                .providerOrderId("legacy-order")
                .amount(new BigDecimal("9.99"))
                .currency(CurrencyCode.USD)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentTransactionRepository.findByProviderOrderId("legacy-order"))
                .thenReturn(Optional.of(transaction));
        when(paymentProvider.captureOrder("legacy-order"))
                .thenReturn(new PaymentProvider.PaymentCaptureResponse(
                        true, "capture-1", "COMPLETED", new BigDecimal("9.99")));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean captured = paymentService.capturePayment("legacy-order");

        assertThat(captured).isTrue();
        assertThat(transaction.getPlanType()).isEqualTo(PlanType.MONTHLY);
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().getPlanType()).isEqualTo(PlanType.MONTHLY);
    }

    @Test
    void capturePaymentRejectsLegacyTransactionWhenPriceNoLongerMatches() {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .paymentMethod("PAYPAL")
                .providerOrderId("unknown-legacy-order")
                .amount(new BigDecimal("19.99"))
                .currency(CurrencyCode.USD)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentTransactionRepository.findByProviderOrderId("unknown-legacy-order"))
                .thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> paymentService.capturePayment("unknown-legacy-order"))
                .hasMessage("Cannot determine plan for legacy payment; create a new payment order");
    }
}
