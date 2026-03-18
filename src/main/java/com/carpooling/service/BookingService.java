package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.PaymentTransaction;
import com.carpooling.model.Ride;
import com.carpooling.model.User;
import com.carpooling.repository.BookingRepository;
import com.carpooling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RideService rideService;

    @Autowired
    private FareService fareService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RealtimeEventService realtimeEventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    public FareEstimate previewFare(Long rideId,
                                    String passengerSource,
                                    String passengerDestination,
                                    int seats) {
        Ride ride = rideService.findById(rideId);
        if (ride == null || ride.getAvailableSeats() < seats || seats <= 0) {
            throw new IllegalArgumentException("Ride unavailable or invalid seats");
        }

        String source = isBlank(passengerSource) ? ride.getSource() : passengerSource;
        String destination = isBlank(passengerDestination) ? ride.getDestination() : passengerDestination;
        int sharingCount = Math.max(ride.getAvailableSeats(), 1);
        return fareService.calculateFare(ride, source, destination, seats, sharingCount);
    }

    public Booking bookSeat(Long rideId, Long passengerId, String passengerName, int seats) {
        BookingPaymentResult result = bookWithPayment(rideId, passengerId, passengerName, seats, null, null, "STRIPE", "sandbox_token");
        return result.getBooking();
    }

    public BookingPaymentResult bookWithPayment(Long rideId,
                                                Long passengerId,
                                                String passengerName,
                                                int seats,
                                                String passengerSource,
                                                String passengerDestination,
                                                String gateway,
                                                String paymentToken) {
        Ride ride = rideService.findById(rideId);
        if (ride == null) {
            throw new IllegalArgumentException("Ride not found");
        }
        if (!"SCHEDULED".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Ride is not available for booking");
        }
        if (seats <= 0 || ride.getAvailableSeats() < seats) {
            throw new IllegalArgumentException("Invalid seat count");
        }

        String source = isBlank(passengerSource) ? ride.getSource() : passengerSource;
        String destination = isBlank(passengerDestination) ? ride.getDestination() : passengerDestination;
        int sharingCount = Math.max(ride.getAvailableSeats(), 1);
        FareEstimate fareEstimate = fareService.calculateFare(ride, source, destination, seats, sharingCount);

        Booking booking = new Booking();
        booking.setRideId(rideId);
        booking.setPassengerId(passengerId);
        booking.setPassengerName(passengerName);
        booking.setSeatsBooked(seats);
        booking.setPassengerSource(source);
        booking.setPassengerDestination(destination);
        booking.setPassengerDistanceKm(fareEstimate.getDistanceKm());
        booking.setBaseFare(fareEstimate.getBaseFare());
        booking.setRatePerKm(fareEstimate.getRatePerKm());
        booking.setFareBeforeSplit(fareEstimate.getFareBeforeSplit());
        booking.setTotalPrice(fareEstimate.getTotalFare());
        booking.setBookingStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking = bookingRepository.save(booking);

        PaymentTransaction payment;
        try {
            payment = paymentService.chargeBooking(
                    booking.getId(),
                    rideId,
                    passengerId,
                    ride.getDriverId(),
                    fareEstimate.getTotalFare(),
                    gateway,
                    paymentToken
            );
        } catch (RuntimeException ex) {
            booking.setPaymentStatus("FAILED");
            booking.setBookingStatus("PAYMENT_FAILED");
            bookingRepository.save(booking);
            throw ex;
        }

        booking.setPaymentStatus(payment.getStatus());
        booking.setPaymentTransactionId(payment.getId());
        if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
            booking.setBookingStatus("PAYMENT_PENDING");
            bookingRepository.save(booking);
            throw new IllegalStateException("Payment is not completed. Please try again.");
        }
        booking.setBookingStatus("CONFIRMED");
        booking = bookingRepository.save(booking);

        ride.setAvailableSeats(ride.getAvailableSeats() - seats);
        rideService.save(ride);

        User passenger = userRepository.findById(passengerId).orElse(null);
        emailNotificationService.sendBookingConfirmationEmail(passenger, booking);

        realtimeEventService.notifyBookingUpdate(rideId, booking.getId(),
                "New booking received from " + passengerName + " for " + seats + " seat(s)");

        BookingPaymentResult result = new BookingPaymentResult();
        result.setBooking(booking);
        result.setPayment(payment);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
