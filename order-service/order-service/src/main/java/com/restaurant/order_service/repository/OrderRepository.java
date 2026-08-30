package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.Order;
import com.restaurant.order_service.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Order> findFirstByBookingId(Long bookingId);
    Optional<Order> findFirstByCustomerIdAndTableIdAndBookingId(
            Long customerId,
            Long tableId,
            Long bookingId
    );
    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end );
    Optional<Order> findFirstByCustomerIdAndBookingId(
            Long customerId,
            Long bookingId
    );
}