package com.carpooling.repository;

import com.carpooling.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findBySourceAndDestinationAndDate(String source, String destination, LocalDate date);
    List<Ride> findByDriverId(Long driverId);
    List<Ride> findByDate(LocalDate date);
    List<Ride> findByDateAndStatus(LocalDate date, String status);
}
