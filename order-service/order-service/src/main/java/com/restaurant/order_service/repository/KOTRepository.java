package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.KOT;
import com.restaurant.order_service.entity.KOTStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KOTRepository
        extends JpaRepository<KOT, Long> {

    Optional<KOT> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<KOT> findByStatusOrderByCreatedAtAsc(
            KOTStatus status
    );

    List<KOT> findAllByOrderByCreatedAtDesc();
}