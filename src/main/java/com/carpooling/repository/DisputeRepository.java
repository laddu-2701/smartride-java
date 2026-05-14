package com.carpooling.repository;

import com.carpooling.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByRaisedByUserIdOrderByCreatedAtDesc(Long userId);
    List<Dispute> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
