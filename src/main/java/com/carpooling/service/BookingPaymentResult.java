package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.PaymentTransaction;

public class BookingPaymentResult {
    private Booking booking;
    private PaymentTransaction payment;

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public PaymentTransaction getPayment() { return payment; }
    public void setPayment(PaymentTransaction payment) { this.payment = payment; }
}