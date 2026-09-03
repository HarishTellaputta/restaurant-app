package com.restaurant.restaurant_service.controller;

import com.restaurant.restaurant_service.dto.TableBookingRequest;
import com.restaurant.restaurant_service.dto.TableBookingResponse;
import com.restaurant.restaurant_service.service.TableBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/table-bookings")
@RequiredArgsConstructor
public class TableBookingController {

    private final TableBookingService bookingService;


    // =====================================================
    // CUSTOMER - CREATE BOOKING
    // =====================================================
    @PostMapping
    public TableBookingResponse createBooking(
            Authentication authentication,
            @Valid @RequestBody TableBookingRequest request
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return bookingService.createBooking(
                customerId,
                request
        );
    }

    // =====================================================
    // CUSTOMER - MY BOOKINGS
    // =====================================================

    @GetMapping("/my-bookings")
    public List<TableBookingResponse> getMyBookings(
            org.springframework.security.core.Authentication authentication
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return bookingService.getMyBookings(customerId);
    }


    // =====================================================
    // GET BOOKING BY ID
    // =====================================================

    @GetMapping("/{id}")
    public TableBookingResponse getBookingById(
            @PathVariable Long id
    ) {

        return bookingService.getBookingById(id);
    }

    // =====================================================
// CUSTOMER - CANCEL BOOKING
// =====================================================

    @PutMapping("/{id}/cancel")
    public TableBookingResponse cancelBooking(
            Authentication authentication,
            @PathVariable Long id
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return bookingService.cancelBooking(
                customerId,
                id
        );
    }
}