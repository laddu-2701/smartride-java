package com.carpooling.repository;

import com.carpooling.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRideId(Long rideId);
    List<Booking> findByPassengerId(Long passengerId);
}
