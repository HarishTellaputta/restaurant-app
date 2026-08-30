package com.restaurant.restaurant_service.repository;

import com.restaurant.restaurant_service.entity.BookingStatus;
import com.restaurant.restaurant_service.entity.TableBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TableBookingRepository
        extends JpaRepository<TableBooking, Long> {

    List<TableBooking> findByCustomerId(Long customerId);

    boolean existsByTableIdAndBookingDateAndBookingTimeAndStatus(
            Long tableId,
            LocalDate bookingDate,
            LocalTime bookingTime,
            BookingStatus status
    );
    @Query("""
    SELECT b
    FROM TableBooking b
    WHERE b.tableId = :tableId
      AND b.status = :status
    ORDER BY b.createdAt DESC
""")
    Optional<TableBooking> findLatestBooking(
            @Param("tableId") Long tableId,
            @Param("status") BookingStatus status
    );
    Optional<TableBooking> findFirstByTableIdAndStatusOrderByCreatedAtDesc(
            Long tableId,
            BookingStatus status
    );
}