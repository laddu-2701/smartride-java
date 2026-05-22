package com.carpooling.service;

import com.carpooling.model.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentServiceTests {

    @Autowired
    private PaymentService paymentService;

    @Test
    void razorpaySandboxChargeCreatesPaidTransaction() {
        PaymentTransaction transaction = paymentService.chargeBooking(
                101L,
                201L,
                301L,
                401L,
                125.75,
                "RAZORPAY",
                ""
        );

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getGateway()).isEqualTo("RAZORPAY");
        assertThat(transaction.getStatus()).isEqualTo("PAID");
        assertThat(transaction.getProviderOrderId()).startsWith("sandbox_razorpay_order_");
        assertThat(transaction.getCurrency()).isEqualTo("INR");
    }

    @Test
    void razorpaySandboxVerificationUpdatesPaymentReference() {
        PaymentTransaction transaction = paymentService.chargeBooking(
                102L,
                202L,
                302L,
                402L,
                88.50,
                "RAZORPAY",
                ""
        );

        PaymentTransaction verified = paymentService.verifyRazorpayPayment(
                String.valueOf(transaction.getBookingId()),
                transaction.getProviderOrderId(),
                "pay_test_123",
                "sig_test_123"
        );

        assertThat(verified.getStatus()).isEqualTo("PAID");
        assertThat(verified.getProviderPaymentId()).isEqualTo("pay_test_123");
        assertThat(verified.getProviderOrderId()).isEqualTo(transaction.getProviderOrderId());
    }
}
