package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.Ride;
import com.carpooling.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RideService rideService;

    public Booking bookSeat(Long rideId, Long passengerId, String passengerName, int seats) {
        Ride ride = rideService.findById(rideId);
        if (ride == null || ride.getAvailableSeats() < seats) {
            return null;
        }
        ride.setAvailableSeats(ride.getAvailableSeats() - seats);
        rideService.save(ride);

        Booking b = new Booking();
        b.setRideId(rideId);
        b.setPassengerId(passengerId);
        b.setPassengerName(passengerName);
        b.setSeatsBooked(seats);
        b.setTotalPrice(seats * ride.getPricePerSeat());
        return bookingRepository.save(b);
    }
}
