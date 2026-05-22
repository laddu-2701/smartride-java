package com.carpooling.controller;

import com.carpooling.model.PaymentTransaction;
import com.carpooling.service.BookingPaymentResult;
import com.carpooling.service.BookingService;
import com.carpooling.service.CustomUserDetails;
import com.carpooling.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/my-transactions")
    public List<PaymentTransaction> myTransactions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return paymentService.getPassengerTransactions(userDetails.getUser().getId());
    }

    @GetMapping("/driver-transactions")
    public List<PaymentTransaction> driverTransactions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return paymentService.getDriverTransactions(userDetails.getUser().getId());
    }

    @GetMapping("/config")
    public Map<String, Object> paymentConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("stripeEnabled", paymentService.isStripeEnabled());
        response.put("stripePublishableKey", paymentService.getStripePublishableKey());
        response.put("razorpayEnabled", paymentService.isRazorpayEnabled());
        response.put("razorpayKeyId", paymentService.getRazorpayKeyId());
        response.put("razorpayCurrency", paymentService.getRazorpayCurrency());
        return response;
    }

    @PostMapping("/webhook")
    public Map<String, String> stripeWebhook(@RequestBody String payload,
                                              @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing Stripe-Signature header");
        }
        paymentService.handleWebhook(payload, signature);
        return Map.of("status", "ok");
    }

    @PostMapping("/razorpay/confirm")
    public BookingPaymentResult razorpayConfirm(@RequestBody Map<String, Object> body) {
        Long bookingId = Long.parseLong(String.valueOf(body.get("bookingId")));
        String orderId = String.valueOf(body.get("razorpay_order_id"));
        String paymentId = String.valueOf(body.get("razorpay_payment_id"));
        String signature = String.valueOf(body.get("razorpay_signature"));
        return bookingService.confirmRazorpayBooking(bookingId, orderId, paymentId, signature);
    }
}