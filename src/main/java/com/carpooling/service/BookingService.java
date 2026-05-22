package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.PaymentTransaction;
import com.carpooling.model.Ride;
import com.carpooling.model.User;
import com.carpooling.repository.BookingRepository;
import com.carpooling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private NotificationService notificationService;

    public FareEstimate previewFare(Long rideId,
                                    String passengerSource,
                                    String passengerDestination,
                                    int seats) {
        Ride ride = rideService.findById(rideId);
        if (ride == null || getEffectiveAvailableSeats(rideId, ride) < seats || seats <= 0) {
            throw new IllegalArgumentException("Ride unavailable or invalid seats");
        }

        String source = isBlank(passengerSource) ? ride.getSource() : passengerSource;
        String destination = isBlank(passengerDestination) ? ride.getDestination() : passengerDestination;
        int sharingCount = getActivePassengerCount(rideId) + 1;
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
        if (seats <= 0 || getEffectiveAvailableSeats(rideId, ride) < seats) {
            throw new IllegalArgumentException("Invalid seat count");
        }

        String source = isBlank(passengerSource) ? ride.getSource() : passengerSource;
        String destination = isBlank(passengerDestination) ? ride.getDestination() : passengerDestination;
        int sharingCount = getActivePassengerCount(rideId) + 1;
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

        BookingPaymentResult result = new BookingPaymentResult();

        // If gateway is NONE/OFFLINE, confirm booking without going through a payment provider.
        if ("NONE".equalsIgnoreCase(gateway) || "OFFLINE".equalsIgnoreCase(gateway)) {
            booking.setPaymentStatus("SKIPPED");
            booking.setBookingStatus("CONFIRMED");
            booking = bookingRepository.save(booking);

            ride.setAvailableSeats(ride.getAvailableSeats() - seats);
            rideService.save(ride);

            User passenger = userRepository.findById(passengerId).orElse(null);
            if (passenger != null) {
                notificationService.notifyBookingConfirmation(passenger, booking);
            }

            realtimeEventService.notifyBookingUpdate(rideId, booking.getId(),
                    "New booking received from " + passengerName + " for " + seats + " seat(s)");

            result.setBooking(booking);
            result.setPayment(null);
            return result;
        }

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

        if ("RAZORPAY".equalsIgnoreCase(payment.getGateway()) && "PENDING".equalsIgnoreCase(payment.getStatus())) {
            booking.setBookingStatus("PAYMENT_PENDING");
            bookingRepository.save(booking);
            result.setBooking(booking);
            result.setPayment(payment);
            return result;
        }

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
        if (passenger != null) {
            notificationService.notifyBookingConfirmation(passenger, booking);
        }

        realtimeEventService.notifyBookingUpdate(rideId, booking.getId(),
                "New booking received from " + passengerName + " for " + seats + " seat(s)");

        result.setBooking(booking);
        result.setPayment(payment);
        return result;
    }

    public BookingPaymentResult confirmRazorpayBooking(Long bookingId,
                                                       String razorpayOrderId,
                                                       String razorpayPaymentId,
                                                       String razorpaySignature) {
        PaymentTransaction payment = paymentService.verifyRazorpayPayment(
                String.valueOf(bookingId),
                razorpayOrderId,
                razorpayPaymentId,
                razorpaySignature
        );

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!"CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())) {
            booking.setPaymentTransactionId(payment.getId());
            booking.setPaymentStatus(payment.getStatus());
            booking.setBookingStatus("CONFIRMED");
            bookingRepository.save(booking);

            Ride ride = rideService.findById(booking.getRideId());
            if (ride != null) {
                ride.setAvailableSeats(Math.max(0, ride.getAvailableSeats() - booking.getSeatsBooked()));
                rideService.save(ride);
            }

            User passenger = userRepository.findById(booking.getPassengerId()).orElse(null);
            if (passenger != null) {
                notificationService.notifyBookingConfirmation(passenger, booking);
            }

            realtimeEventService.notifyBookingUpdate(booking.getRideId(), booking.getId(),
                    "Payment confirmed for booking " + booking.getId());
        }

        BookingPaymentResult result = new BookingPaymentResult();
        result.setBooking(booking);
        result.setPayment(payment);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int getActivePassengerCount(Long rideId) {
        long active = bookingRepository.countByRideIdAndBookingStatusIn(
                rideId,
                List.of("CONFIRMED", "PENDING_PAYMENT", "PENDING")
        );
        return (int) Math.max(active, 0);
    }

    private int getEffectiveAvailableSeats(Long rideId, Ride ride) {
        long pendingReservations = bookingRepository.sumSeatsBookedByRideIdAndBookingStatusIn(
                rideId,
                List.of("PENDING_PAYMENT", "PENDING")
        );
        return (int) Math.max(ride.getAvailableSeats() - pendingReservations, 0);
    }
}
