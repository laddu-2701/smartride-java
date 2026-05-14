package com.carpooling.repository;

import com.carpooling.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
    List<PaymentTransaction> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<PaymentTransaction> findByRideIdOrderByCreatedAtDesc(Long rideId);
    List<PaymentTransaction> findByRideId(Long rideId);
    PaymentTransaction findFirstByProviderOrderId(String providerOrderId);
    PaymentTransaction findFirstByProviderPaymentId(String providerPaymentId);

    @Query("select coalesce(sum(p.amount), 0) from PaymentTransaction p where upper(p.status) = upper(:status)")
    Double sumAmountByStatus(@Param("status") String status);
}