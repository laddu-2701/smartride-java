package com.carpooling.repository;

import com.carpooling.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByRideIdAndReviewerIdAndRevieweeId(Long rideId, Long reviewerId, Long revieweeId);
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);
    List<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);
    List<Review> findByRideIdOrderByCreatedAtDesc(Long rideId);

    @Query("select coalesce(avg(r.stars), 0) from Review r where r.revieweeId = :userId")
    Double averageStarsByReviewee(@Param("userId") Long userId);

    long countByRevieweeId(Long revieweeId);
}
