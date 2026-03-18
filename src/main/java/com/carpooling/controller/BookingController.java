package com.carpooling.controller;

import com.carpooling.model.Booking;
import com.carpooling.repository.BookingRepository;
import com.carpooling.service.BookingPaymentResult;
import com.carpooling.service.BookingService;
import com.carpooling.service.CustomUserDetails;
import com.carpooling.service.FareEstimate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @PostMapping("/book")
    public Booking bookSeat(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestBody Map<String, Object> body) {
        try {
            Long rideId = Long.parseLong(String.valueOf(body.get("rideId")));
            int seats = Integer.parseInt(String.valueOf(body.get("seats")));
            return bookingService.bookSeat(rideId, userDetails.getUser().getId(), userDetails.getUsername(), seats);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid rideId or seats format");
        } catch (Exception e) {
            throw new RuntimeException("Booking failed: " + e.getMessage());
        }
    }

    @GetMapping("/fare-preview")
    public FareEstimate farePreview(@RequestParam Long rideId,
                                    @RequestParam int seats,
                                    @RequestParam(required = false) String source,
                                    @RequestParam(required = false) String destination) {
        return bookingService.previewFare(rideId, source, destination, seats);
    }

    @PostMapping("/book-with-payment")
    public BookingPaymentResult bookWithPayment(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestBody Map<String, Object> body) {
        try {
            Long rideId = Long.parseLong(String.valueOf(body.get("rideId")));
            int seats = Integer.parseInt(String.valueOf(body.get("seats")));
            String source = body.get("source") == null ? null : String.valueOf(body.get("source"));
            String destination = body.get("destination") == null ? null : String.valueOf(body.get("destination"));
            String gateway = body.get("gateway") == null ? "STRIPE" : String.valueOf(body.get("gateway"));
            String paymentToken = body.get("paymentToken") == null ? "" : String.valueOf(body.get("paymentToken"));
            return bookingService.bookWithPayment(
                    rideId,
                    userDetails.getUser().getId(),
                    userDetails.getUsername(),
                    seats,
                    source,
                    destination,
                    gateway,
                    paymentToken
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid rideId or seats format");
        }
    }

    @GetMapping("/my-bookings")
    public List<Booking> getMyBookings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return bookingRepository.findByPassengerId(userDetails.getUser().getId());
    }

    @GetMapping("/ride-bookings")
    public List<Booking> getRideBookings(@RequestParam Long rideId) {
        return bookingRepository.findByRideId(rideId);
    }
}
