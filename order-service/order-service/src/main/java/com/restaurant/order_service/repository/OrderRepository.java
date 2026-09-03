package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.Order;
import com.restaurant.order_service.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Order> findFirstByBookingId(Long bookingId);
    Optional<Order> findFirstByCustomerIdAndTableIdAndBookingId(
            Long customerId,
            Long tableId,
            Long bookingId
    );
    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Query("""
    SELECT o
    FROM Order o
    WHERE o.createdAt >= :start
      AND o.createdAt < :end
    ORDER BY o.createdAt DESC
""")
    List<Order> findTodayOrders(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<Order> findFirstByCustomerIdAndBookingId(
            Long customerId,
            Long bookingId
    );
}