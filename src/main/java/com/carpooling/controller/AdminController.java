package com.carpooling.controller;

import com.carpooling.model.Dispute;
import com.carpooling.model.Role;
import com.carpooling.model.User;
import com.carpooling.model.Review;
import com.carpooling.repository.*;
import com.carpooling.service.DisputeService;
import com.carpooling.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalRides", rideRepository.count());
        data.put("totalBookings", bookingRepository.count());
        data.put("totalPayments", paymentTransactionRepository.count());
        data.put("totalEarnings", round(paymentTransactionRepository.sumAmountByStatus("PAID")));
        data.put("activeUsers", userRepository.count() - userRepository.countByBlockedTrue());
        data.put("blockedUsers", userRepository.countByBlockedTrue());
        data.put("drivers", userRepository.countByRole(Role.DRIVER));
        data.put("verifiedDrivers", userRepository.countByRoleAndDriverVerifiedTrue(Role.DRIVER));
        data.put("rideCancellations", rideRepository.countByStatus("CANCELLED"));
        data.put("disputes", disputeRepository.count());
        data.put("openDisputes", disputeRepository.countByStatus("OPEN"));
        return data;
    }

    @GetMapping("/rides")
    public Object rides() {
        return rideRepository.findAll();
    }

    @GetMapping("/bookings")
    public Object bookings() {
        return bookingRepository.findAll();
    }

    @GetMapping("/reviews")
    public Object reviews() {
        return reviewRepository.findAll();
    }

    @GetMapping("/payments")
    public Object payments() {
        return paymentTransactionRepository.findAll();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return userRepository.findAll().stream().map(this::toUserRow).collect(Collectors.toList());
    }

    @PatchMapping("/users/{userId}/block")
    public Map<String, Object> blockUser(@PathVariable Long userId,
                                         @RequestBody(required = false) Map<String, Object> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean blocked = body != null && body.get("blocked") != null
                ? Boolean.parseBoolean(String.valueOf(body.get("blocked")))
                : true;
        user.setBlocked(blocked);
        userRepository.save(user);
        return Map.of("userId", user.getId(), "blocked", user.isBlocked());
    }

    @PatchMapping("/users/{userId}/verify-driver")
    public Map<String, Object> verifyDriver(@PathVariable Long userId,
                                            @RequestBody(required = false) Map<String, Object> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException("Selected user is not a driver");
        }
        boolean verified = body != null && body.get("verified") != null
                ? Boolean.parseBoolean(String.valueOf(body.get("verified")))
                : true;
        user.setDriverVerified(verified);
        userRepository.save(user);
        return Map.of("userId", user.getId(), "driverVerified", user.isDriverVerified());
    }

    @GetMapping("/disputes")
    public List<Dispute> allDisputes() {
        return disputeService.allDisputes();
    }

    @PatchMapping("/disputes/{disputeId}/resolve")
    public Dispute resolveDispute(@PathVariable Long disputeId) {
        return disputeService.resolveDispute(disputeId);
    }

    private Map<String, Object> toUserRow(User user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", user.getId());
        row.put("name", user.getName());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhone());
        row.put("role", user.getRole());
        row.put("blocked", user.isBlocked());
        row.put("driverVerified", user.isDriverVerified());
        row.put("vehicleModel", user.getVehicleModel());
        row.put("licensePlate", user.getLicensePlate());

        // Per-user stats for admin view
        long ridesAsDriver = rideRepository.findByDriverId(user.getId()).size();
        long bookingsAsPassenger = bookingRepository.findByPassengerId(user.getId()).size();
        Map<String, Object> rating = reviewService.getRatingSummary(user.getId());

        row.put("ridesAsDriver", ridesAsDriver);
        row.put("bookingsAsPassenger", bookingsAsPassenger);
        row.put("averageRating", rating.get("averageRating"));
        row.put("reviewCount", rating.get("reviewCount"));
        return row;
    }

    private double round(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
