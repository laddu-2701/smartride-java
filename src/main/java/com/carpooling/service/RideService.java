package com.carpooling.service;

import com.carpooling.model.Ride;
import com.carpooling.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RideService {
    @Autowired
    private RideRepository rideRepository;

    public Ride postRide(Ride ride) {
        return rideRepository.save(ride);
    }

    public List<Ride> searchRides(String source, String destination, LocalDate date) {
        return rideRepository.findBySourceAndDestinationAndDate(source, destination, date);
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
}
