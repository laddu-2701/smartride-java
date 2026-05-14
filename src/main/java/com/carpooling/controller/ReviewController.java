package com.carpooling.controller;

import com.carpooling.model.Review;
import com.carpooling.service.CustomUserDetails;
import com.carpooling.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody Map<String, Object> body) {
        try {
            Long rideId = Long.parseLong(String.valueOf(body.get("rideId")));
            Long revieweeId = Long.parseLong(String.valueOf(body.get("revieweeId")));
            int stars = Integer.parseInt(String.valueOf(body.get("stars")));
            Long bookingId = body.get("bookingId") == null ? null : Long.parseLong(String.valueOf(body.get("bookingId")));
            String comments = body.get("comments") == null ? "" : String.valueOf(body.get("comments"));

            Review review = reviewService.submitReview(
                    userDetails.getUser().getId(),
                    userDetails.getUser().getRole(),
                    rideId,
                    revieweeId,
                    bookingId,
                    stars,
                    comments
            );
            return ResponseEntity.ok(review);
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body("Invalid feedback payload: ride, reviewee, booking and stars must be numbers");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @GetMapping("/my/received")
    public List<Review> myReceived(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return reviewService.getReceivedReviews(userDetails.getUser().getId());
    }

    @GetMapping("/my/given")
    public List<Review> myGiven(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return reviewService.getGivenReviews(userDetails.getUser().getId());
    }

    @GetMapping("/my/pending")
    public List<Map<String, Object>> pendingTargets(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return reviewService.getPendingReviewTargets(userDetails.getUser().getId(), userDetails.getUser().getRole());
    }

    @GetMapping("/summary/{userId}")
    public Map<String, Object> summary(@PathVariable Long userId) {
        return reviewService.getRatingSummary(userId);
    }
}
