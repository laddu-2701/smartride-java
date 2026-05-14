package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.Review;
import com.carpooling.model.Ride;
import com.carpooling.model.Role;
import com.carpooling.repository.BookingRepository;
import com.carpooling.repository.ReviewRepository;
import com.carpooling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RideService rideService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    public Review submitReview(Long reviewerId,
                               Role reviewerRole,
                               Long rideId,
                               Long revieweeId,
                               Long bookingId,
                               int stars,
                               String comments) {
        if (reviewerId == null || revieweeId == null || rideId == null) {
            throw new IllegalArgumentException("Missing required review fields");
        }
        if (reviewerId.equals(revieweeId)) {
            throw new IllegalArgumentException("You cannot review yourself");
        }
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be between 1 and 5");
        }

        Ride ride = rideService.findById(rideId);
        if (ride == null) {
            throw new IllegalArgumentException("Ride not found");
        }
        if (!"COMPLETED".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Reviews are allowed only after ride completion");
        }

        Long resolvedBookingId;
        if (reviewerRole == Role.PASSENGER) {
            Booking passengerBooking = resolvePassengerBooking(rideId, reviewerId, bookingId);
            if (passengerBooking == null) {
                throw new SecurityException("Passenger is not part of this ride");
            }
            if (!Objects.equals(ride.getDriverId(), revieweeId)) {
                throw new IllegalArgumentException("Passengers can only review the ride driver");
            }
            resolvedBookingId = passengerBooking.getId();
        } else if (reviewerRole == Role.DRIVER) {
            if (!Objects.equals(ride.getDriverId(), reviewerId)) {
                throw new SecurityException("Driver is not authorized for this ride");
            }
            Booking passengerBooking = resolvePassengerBooking(rideId, revieweeId, bookingId);
            if (passengerBooking == null) {
                throw new IllegalArgumentException("Drivers can only review passengers from this ride");
            }
            resolvedBookingId = passengerBooking.getId();
        } else if (reviewerRole == Role.ADMIN) {
            // Admin reviews: allow platform admins to drop a review for any user
            // on a completed ride. Booking linkage is optional and not enforced.
            resolvedBookingId = bookingId;
        } else {
            throw new SecurityException("Only drivers, passengers and admins can submit reviews");
        }

        if (reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(rideId, reviewerId, revieweeId)) {
            throw new IllegalStateException("You have already reviewed this user for the selected ride");
        }

        Review review = new Review();
        review.setRideId(rideId);
        review.setBookingId(resolvedBookingId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setReviewerRole(reviewerRole.name());
        review.setStars(stars);
        review.setComments(comments == null ? "" : comments.trim());
        return reviewRepository.save(review);
    }

    public List<Review> getReceivedReviews(Long userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);
    }

    public List<Review> getGivenReviews(Long userId) {
        return reviewRepository.findByReviewerIdOrderByCreatedAtDesc(userId);
    }

    public Map<String, Object> getRatingSummary(Long userId) {
        Double avg = reviewRepository.averageStarsByReviewee(userId);
        long total = reviewRepository.countByRevieweeId(userId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("averageRating", round(avg == null ? 0.0 : avg));
        summary.put("reviewCount", total);
        return summary;
    }

    public List<Map<String, Object>> getPendingReviewTargets(Long reviewerId, Role reviewerRole) {
        List<Map<String, Object>> targets = new ArrayList<>();

        if (reviewerRole == Role.PASSENGER) {
            List<Booking> myBookings = bookingRepository.findByPassengerId(reviewerId);
            for (Booking booking : myBookings) {
                Ride ride = rideService.findById(booking.getRideId());
                if (ride == null || !"COMPLETED".equalsIgnoreCase(ride.getStatus())) {
                    continue;
                }
                Long revieweeId = ride.getDriverId();
                if (revieweeId == null || reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(ride.getId(), reviewerId, revieweeId)) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("rideId", ride.getId());
                item.put("bookingId", booking.getId());
                item.put("revieweeId", revieweeId);
                item.put("revieweeName", ride.getDriverName());
                item.put("reviewerRole", "PASSENGER");
                item.put("route", ride.getSource() + " -> " + ride.getDestination());
                targets.add(item);
            }
        }

        if (reviewerRole == Role.DRIVER) {
            List<Ride> rides = rideService.getRidesByDriverId(reviewerId);
            for (Ride ride : rides) {
                if (!"COMPLETED".equalsIgnoreCase(ride.getStatus())) {
                    continue;
                }
                List<Booking> bookings = bookingRepository.findByRideId(ride.getId());
                for (Booking booking : bookings) {
                    Long revieweeId = booking.getPassengerId();
                    if (revieweeId == null || reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(ride.getId(), reviewerId, revieweeId)) {
                        continue;
                    }
                    String revieweeName = userRepository.findById(revieweeId).map(u -> u.getName()).orElse("Passenger");
                    Map<String, Object> item = new HashMap<>();
                    item.put("rideId", ride.getId());
                    item.put("bookingId", booking.getId());
                    item.put("revieweeId", revieweeId);
                    item.put("revieweeName", revieweeName);
                    item.put("reviewerRole", "DRIVER");
                    item.put("route", ride.getSource() + " -> " + ride.getDestination());
                    targets.add(item);
                }
            }
        }

        return targets;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Booking resolvePassengerBooking(Long rideId, Long passengerId, Long bookingId) {
        if (bookingId != null) {
            Booking byId = bookingRepository.findById(bookingId).orElse(null);
            if (byId != null
                    && Objects.equals(byId.getRideId(), rideId)
                    && Objects.equals(byId.getPassengerId(), passengerId)) {
                return byId;
            }
        }
        return bookingRepository.findFirstByRideIdAndPassengerId(rideId, passengerId);
    }
}
