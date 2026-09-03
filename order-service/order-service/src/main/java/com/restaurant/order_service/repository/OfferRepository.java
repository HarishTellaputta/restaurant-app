package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OfferRepository
        extends JpaRepository<Offer, Long> {

    List<Offer> findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}