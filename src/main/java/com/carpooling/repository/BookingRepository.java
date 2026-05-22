package com.carpooling.repository;

import com.carpooling.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRideId(Long rideId);
    List<Booking> findByPassengerId(Long passengerId);
    long countByRideIdAndBookingStatusIn(Long rideId, Collection<String> statuses);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(b.seatsBooked), 0) from Booking b where b.rideId = :rideId and upper(b.bookingStatus) in :statuses")
    long sumSeatsBookedByRideIdAndBookingStatusIn(@org.springframework.data.repository.query.Param("rideId") Long rideId,
                                                 @org.springframework.data.repository.query.Param("statuses") Collection<String> statuses);
    List<Booking> findByBookingStatusIgnoreCase(String bookingStatus);
    Booking findFirstByRideIdAndPassengerId(Long rideId, Long passengerId);
}
