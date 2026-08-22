package com.restaurant.restaurant_service.repository;

import com.restaurant.restaurant_service.entity.BookingStatus;
import com.restaurant.restaurant_service.entity.TableBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TableBookingRepository
        extends JpaRepository<TableBooking, Long> {

    List<TableBooking> findByCustomerId(Long customerId);

    boolean existsByTableIdAndBookingDateAndBookingTimeAndStatus(
            Long tableId,
            LocalDate bookingDate,
            LocalTime bookingTime,
            BookingStatus status
    );
}