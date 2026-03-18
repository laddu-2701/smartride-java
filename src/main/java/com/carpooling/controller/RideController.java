package com.carpooling.controller;

import com.carpooling.model.Booking;
import com.carpooling.model.Ride;
import com.carpooling.model.User;
import com.carpooling.repository.BookingRepository;
import com.carpooling.repository.UserRepository;
import com.carpooling.service.EmailNotificationService;
import com.carpooling.service.RideService;
import com.carpooling.service.CustomUserDetails;
import com.carpooling.service.RealtimeEventService;
import com.carpooling.service.RouteMatchingService;
import com.carpooling.service.RideMatchResult;
import com.carpooling.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rides")
public class RideController {
    @Autowired
    private RideService rideService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteMatchingService routeMatchingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private RealtimeEventService realtimeEventService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/post")
    public Ride postRide(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody Map<String, String> body) {
        User driver = userDetails.getUser();

        Ride ride = new Ride();
        ride.setSource(body.get("source"));
        ride.setDestination(body.get("destination"));
        try {
            ride.setDate(LocalDate.parse(body.get("date")));
            ride.setTime(java.time.LocalTime.parse(body.get("time")));
            ride.setAvailableSeats(Integer.parseInt(body.get("availableSeats")));
            ride.setPricePerSeat(Double.parseDouble(body.get("pricePerSeat")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid input format");
        }
        
        // auto-fill driver info from profile
        ride.setDriverId(driver.getId());
        ride.setDriverName(driver.getName());
        ride.setVehicleModel(driver.getVehicleModel());
        ride.setLicensePlate(driver.getLicensePlate());
        
        return rideService.postRide(ride);
    }

    @GetMapping("/search")
    public List<Ride> search(@RequestParam String source,
                             @RequestParam String destination,
                             @RequestParam String date) {
        if (source == null || source.trim().isEmpty() ||
            destination == null || destination.trim().isEmpty() ||
            date == null || date.trim().isEmpty()) {
            return List.of();
        }
        try {
            LocalDate searchDate = LocalDate.parse(date);
            return rideService.searchRides(source, destination, searchDate);
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/match")
    public List<RideMatchResult> matchRoutes(@RequestParam String source,
                                             @RequestParam String destination,
                                             @RequestParam String date) {
        if (source == null || source.trim().isEmpty() || destination == null || destination.trim().isEmpty()) {
            return List.of();
        }
        try {
            LocalDate searchDate = LocalDate.parse(date);
            return routeMatchingService.findMatches(source, destination, searchDate);
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/my-rides")
    public List<Ride> getMyRides(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rideService.getRidesByDriverId(userDetails.getUser().getId());
    }

    @PostMapping("/{rideId}/cancel")
    public Ride cancelRide(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @PathVariable Long rideId) {
        Ride ride = rideService.findById(rideId);
        if (ride == null) {
            throw new IllegalArgumentException("Ride not found");
        }
        if (!ride.getDriverId().equals(userDetails.getUser().getId())) {
            throw new SecurityException("You can only cancel your own ride");
        }
        ride.setStatus("CANCELLED");
        Ride saved = rideService.save(ride);
        realtimeEventService.notifyRideUpdate(rideId, "CANCELLED", "Driver cancelled the ride");

        // Email every passenger who has a confirmed booking on this ride
        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        for (Booking booking : bookings) {
            if ("CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())) {
                userRepository.findById(booking.getPassengerId()).ifPresent(
                        passenger -> emailNotificationService.sendRideCancelledEmail(passenger, booking)
                );
            }
        }
        return saved;
    }

    @PutMapping("/{rideId}/reschedule")
    public Ride rescheduleRide(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @PathVariable Long rideId,
                               @RequestBody Map<String, String> body) {
        Ride ride = rideService.findById(rideId);
        if (ride == null) {
            throw new IllegalArgumentException("Ride not found");
        }
        if (!ride.getDriverId().equals(userDetails.getUser().getId())) {
            throw new SecurityException("You can only reschedule your own ride");
        }
        try {
            if (body.containsKey("date")) {
                ride.setDate(LocalDate.parse(body.get("date")));
            }
            if (body.containsKey("time")) {
                ride.setTime(java.time.LocalTime.parse(body.get("time")));
            }
            ride.setStatus("SCHEDULED");
            Ride saved = rideService.save(ride);
            realtimeEventService.notifyRideUpdate(rideId, "RESCHEDULED", "Ride date/time has been updated by driver");

            // Email every passenger who has a confirmed booking on this ride
            final String newDateStr = saved.getDate() != null ? saved.getDate().toString() : "";
            final String newTimeStr = saved.getTime() != null ? saved.getTime().toString() : "";
            List<Booking> bookings = bookingRepository.findByRideId(rideId);
            for (Booking booking : bookings) {
                if ("CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())) {
                    userRepository.findById(booking.getPassengerId()).ifPresent(
                            passenger -> emailNotificationService.sendRideRescheduledEmail(passenger, booking, newDateStr, newTimeStr)
                    );
                }
            }
            return saved;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date/time format");
        }
    }

    @PostMapping("/{rideId}/complete")
    public Ride completeRide(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @PathVariable Long rideId) {
        Ride ride = rideService.findById(rideId);
        if (ride == null) {
            throw new IllegalArgumentException("Ride not found");
        }
        if (!ride.getDriverId().equals(userDetails.getUser().getId())) {
            throw new SecurityException("You can only complete your own ride");
        }

        ride.setStatus("COMPLETED");
        Ride saved = rideService.save(ride);
        paymentService.releaseDriverPayoutsForRide(rideId);
        realtimeEventService.notifyRideUpdate(rideId, "COMPLETED", "Ride completed. Driver payout transferred.");
        return saved;
    }
}
