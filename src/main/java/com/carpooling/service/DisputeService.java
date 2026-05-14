package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.Dispute;
import com.carpooling.model.Ride;
import com.carpooling.repository.BookingRepository;
import com.carpooling.repository.DisputeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DisputeService {
    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private RideService rideService;

    @Autowired
    private BookingRepository bookingRepository;

    public Dispute raiseDispute(Long userId, Long rideId, Long bookingId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }

        Ride ride = rideService.findById(rideId);
        if (ride == null) {
            throw new IllegalArgumentException("Ride not found");
        }

        if (!Objects.equals(ride.getDriverId(), userId)) {
            Booking booking = bookingRepository.findById(bookingId == null ? -1L : bookingId).orElse(null);
            if (booking == null || !Objects.equals(booking.getPassengerId(), userId) || !Objects.equals(booking.getRideId(), rideId)) {
                throw new SecurityException("You can only raise disputes for rides you are part of");
            }
        }

        Dispute dispute = new Dispute();
        dispute.setRideId(rideId);
        dispute.setBookingId(bookingId);
        dispute.setRaisedByUserId(userId);
        dispute.setReason(reason.trim());
        return disputeRepository.save(dispute);
    }

    public List<Dispute> myDisputes(Long userId) {
        return disputeRepository.findByRaisedByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Dispute> allDisputes() {
        return disputeRepository.findAll();
    }

    public Dispute resolveDispute(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        dispute.setStatus("RESOLVED");
        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
