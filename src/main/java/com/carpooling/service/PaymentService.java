package com.carpooling.service;

import com.carpooling.model.PaymentTransaction;
import com.carpooling.repository.PaymentTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PaymentService {
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Value("${stripe.secret.key:}")
    private String stripeSecretKey;

    @Value("${stripe.publishable.key:}")
    private String stripePublishableKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    public PaymentTransaction chargeBooking(Long bookingId,
                                            Long rideId,
                                            Long passengerId,
                                            Long driverId,
                                            double amount,
                                            String gateway,
                                            String paymentToken) {
        String normalizedGateway = normalizeGateway(gateway);
        if (paymentToken == null || paymentToken.trim().length() < 6) {
            throw new IllegalArgumentException("Invalid payment token");
        }

        if (!"STRIPE".equals(normalizedGateway)) {
            throw new IllegalArgumentException("This environment currently supports real STRIPE payments only");
        }

        // Sandbox / demo mode: when no real Stripe keys are present, simulate a successful payment
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            PaymentTransaction sandboxTx = new PaymentTransaction();
            sandboxTx.setBookingId(bookingId);
            sandboxTx.setRideId(rideId);
            sandboxTx.setPassengerId(passengerId);
            sandboxTx.setDriverId(driverId);
            sandboxTx.setAmount(round(amount));
            sandboxTx.setCurrency("USD");
            sandboxTx.setGateway("STRIPE");
            sandboxTx.setProviderOrderId("sandbox_order_" + bookingId);
            sandboxTx.setProviderPaymentId("sandbox_payment_" + bookingId);
            sandboxTx.setStatus("PAID");
            sandboxTx.setPayoutStatus("HELD");
            sandboxTx.setPaidAt(LocalDateTime.now());
            return paymentTransactionRepository.save(sandboxTx);
        }

        Stripe.apiKey = stripeSecretKey;
        PaymentIntent intent = createAndConfirmPaymentIntent(amount, paymentToken, bookingId, rideId, passengerId, driverId);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBookingId(bookingId);
        transaction.setRideId(rideId);
        transaction.setPassengerId(passengerId);
        transaction.setDriverId(driverId);
        transaction.setAmount(round(amount));
        transaction.setCurrency("USD");
        transaction.setGateway(normalizedGateway);
        transaction.setProviderOrderId(intent.getId());
        transaction.setProviderPaymentId(intent.getLatestCharge());
        transaction.setStatus(mapStripeStatus(intent.getStatus()));
        transaction.setPayoutStatus("HELD");
        if ("PAID".equals(transaction.getStatus())) {
            transaction.setPaidAt(LocalDateTime.now());
        }
        transaction.setReceiptUrl(resolveReceiptUrl(intent));
        return paymentTransactionRepository.save(transaction);
    }

    public String getStripePublishableKey() {
        return stripePublishableKey == null ? "" : stripePublishableKey;
    }

    public boolean isStripeEnabled() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank() && stripePublishableKey != null && !stripePublishableKey.isBlank();
    }

    public void handleWebhook(String payload, String stripeSignature) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        if (!event.getType().startsWith("payment_intent.")) {
            return;
        }

        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (intent == null) {
            return;
        }

        PaymentTransaction transaction = paymentTransactionRepository.findFirstByProviderOrderId(intent.getId());
        if (transaction == null) {
            return;
        }

        transaction.setStatus(mapStripeStatus(intent.getStatus()));
        transaction.setProviderPaymentId(intent.getLatestCharge());
        if ("PAID".equals(transaction.getStatus())) {
            transaction.setPaidAt(LocalDateTime.now());
        }
        transaction.setReceiptUrl(resolveReceiptUrl(intent));
        paymentTransactionRepository.save(transaction);
    }

    public void releaseDriverPayoutsForRide(Long rideId) {
        List<PaymentTransaction> list = paymentTransactionRepository.findByRideId(rideId);
        for (PaymentTransaction transaction : list) {
            if ("PAID".equalsIgnoreCase(transaction.getStatus())) {
                transaction.setPayoutStatus("TRANSFERRED");
                paymentTransactionRepository.save(transaction);
            }
        }
    }

    public List<PaymentTransaction> getPassengerTransactions(Long passengerId) {
        return paymentTransactionRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId);
    }

    public List<PaymentTransaction> getDriverTransactions(Long driverId) {
        return paymentTransactionRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
    }

    private String normalizeGateway(String gateway) {
        if (gateway == null || gateway.isBlank()) {
            return "STRIPE";
        }
        String normalized = gateway.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("STRIPE") && !normalized.equals("RAZORPAY") && !normalized.equals("PAYPAL")) {
            throw new IllegalArgumentException("Unsupported gateway. Use STRIPE, RAZORPAY, or PAYPAL");
        }
        return normalized;
    }

    private PaymentIntent createAndConfirmPaymentIntent(double amount,
                                                        String paymentMethodId,
                                                        Long bookingId,
                                                        Long rideId,
                                                        Long passengerId,
                                                        Long driverId) {
        try {
            long amountInCents = Math.max(50L, Math.round(round(amount) * 100));
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setDescription("Carpooling booking payment")
                    .putMetadata("bookingId", String.valueOf(bookingId))
                    .putMetadata("rideId", String.valueOf(rideId))
                    .putMetadata("passengerId", String.valueOf(passengerId))
                    .putMetadata("driverId", String.valueOf(driverId))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();
            return PaymentIntent.create(params);
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe charge failed: " + e.getMessage());
        }
    }

    private String mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return "PENDING";
        }
        String normalized = stripeStatus.toLowerCase(Locale.ROOT);
        if ("succeeded".equals(normalized)) {
            return "PAID";
        }
        if ("requires_payment_method".equals(normalized) || "canceled".equals(normalized)) {
            return "FAILED";
        }
        return "PENDING";
    }

    private String resolveReceiptUrl(PaymentIntent intent) {
        if (intent == null || intent.getLatestCharge() == null || intent.getLatestCharge().isBlank()) {
            return null;
        }
        try {
            Charge charge = Charge.retrieve(intent.getLatestCharge());
            return charge.getReceiptUrl();
        } catch (Exception ignored) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}