package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository
        extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(
            String code
    );
}