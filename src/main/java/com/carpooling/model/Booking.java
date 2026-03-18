package com.carpooling.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rideId;
    private Long passengerId;
    private String passengerName;
    private int seatsBooked;
    private double totalPrice;
    private String passengerSource;
    private String passengerDestination;
    private double passengerDistanceKm;
    private double baseFare;
    private double ratePerKm;
    private double fareBeforeSplit;
    private String bookingStatus;
    private String paymentStatus;
    private Long paymentTransactionId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }
    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public int getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(int seatsBooked) { this.seatsBooked = seatsBooked; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getPassengerSource() { return passengerSource; }
    public void setPassengerSource(String passengerSource) { this.passengerSource = passengerSource; }
    public String getPassengerDestination() { return passengerDestination; }
    public void setPassengerDestination(String passengerDestination) { this.passengerDestination = passengerDestination; }
    public double getPassengerDistanceKm() { return passengerDistanceKm; }
    public void setPassengerDistanceKm(double passengerDistanceKm) { this.passengerDistanceKm = passengerDistanceKm; }
    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }
    public double getRatePerKm() { return ratePerKm; }
    public void setRatePerKm(double ratePerKm) { this.ratePerKm = ratePerKm; }
    public double getFareBeforeSplit() { return fareBeforeSplit; }
    public void setFareBeforeSplit(double fareBeforeSplit) { this.fareBeforeSplit = fareBeforeSplit; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public Long getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(Long paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void init() {
        this.createdAt = LocalDateTime.now();
        if (this.bookingStatus == null || this.bookingStatus.isBlank()) {
            this.bookingStatus = "CONFIRMED";
        }
        if (this.paymentStatus == null || this.paymentStatus.isBlank()) {
            this.paymentStatus = "PENDING";
        }
    }
}
