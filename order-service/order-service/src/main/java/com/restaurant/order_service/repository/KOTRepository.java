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
    /** * Get all KOTs belonging to an order. */
    List<KOT> findByOrderIdOrderByCreatedAtAsc( Long orderId );
    /** * Get final KOT of an order. */
    Optional<KOT> findByOrderIdAndFinalKotTrue( Long orderId );
    /** * Check whether final KOT already exists. */
    boolean existsByOrderIdAndFinalKotTrue( Long orderId );
    /** * Kitchen KOTs by status. */
    List<KOT> findByStatusOrderByCreatedAtAsc( KOTStatus status );
    /** * All KOTs. */
    List<KOT> findAllByOrderByCreatedAtDesc();
}