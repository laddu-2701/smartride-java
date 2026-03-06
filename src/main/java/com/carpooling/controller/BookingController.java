package com.carpooling.controller;

import com.carpooling.model.Booking;
import com.carpooling.repository.BookingRepository;
import com.carpooling.service.BookingService;
import com.carpooling.service.CustomUserDetails;
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
                            @RequestBody Map<String, String> body) {
        try {
            Long rideId = Long.parseLong(body.get("rideId"));
            int seats = Integer.parseInt(body.get("seats"));
            return bookingService.bookSeat(rideId, userDetails.getUser().getId(), userDetails.getUsername(), seats);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid rideId or seats format");
        } catch (Exception e) {
            throw new RuntimeException("Booking failed: " + e.getMessage());
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
