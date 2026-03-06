package com.carpooling.controller;

import com.carpooling.model.Ride;
import com.carpooling.model.User;
import com.carpooling.repository.UserRepository;
import com.carpooling.service.RideService;
import com.carpooling.service.CustomUserDetails;
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

    @GetMapping("/my-rides")
    public List<Ride> getMyRides(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return rideService.getRidesByDriverId(userDetails.getUser().getId());
    }
}
