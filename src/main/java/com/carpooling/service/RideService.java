package com.carpooling.service;

import com.carpooling.model.Ride;
import com.carpooling.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideService {
    @Autowired
    private RideRepository rideRepository;

    public Ride postRide(Ride ride) {
        return rideRepository.save(ride);
    }

    public List<Ride> searchRides(String source, String destination, LocalDate date) {
        return rideRepository.findBySourceAndDestinationAndDate(source, destination, date)
                .stream()
                .filter(ride -> "SCHEDULED".equalsIgnoreCase(ride.getStatus()))
                .collect(Collectors.toList());
    }

    public Ride findById(Long id) {
        return rideRepository.findById(id).orElse(null);
    }

    public Ride save(Ride ride) {
        return rideRepository.save(ride);
    }

    public List<Ride> getRidesByDriverId(Long driverId) {
        return rideRepository.findByDriverId(driverId);
    }

    public List<Ride> getRidesByDate(LocalDate date) {
        return rideRepository.findByDateAndStatus(date, "SCHEDULED");
    }
}
