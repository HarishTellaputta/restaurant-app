package com.restaurant.restaurant_service.service;

import com.restaurant.restaurant_service.dto.TableBookingRequest;
import com.restaurant.restaurant_service.dto.TableBookingResponse;
import com.restaurant.restaurant_service.entity.BookingStatus;
import com.restaurant.restaurant_service.entity.RestaurantTable;
import com.restaurant.restaurant_service.entity.TableBooking;
import com.restaurant.restaurant_service.entity.TableStatus;
import com.restaurant.restaurant_service.repository.RestaurantTableRepository;
import com.restaurant.restaurant_service.repository.TableBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableBookingService {

    private final TableBookingRepository bookingRepository;
    private final RestaurantTableRepository tableRepository;


    // =========================
    // CREATE BOOKING
    // =========================

    @Transactional
    public TableBookingResponse createBooking(
            Long customerId,
            TableBookingRequest request
    ) {

        log.info(
                "Creating table booking. customerId={}, tableId={}, date={}, time={}, guests={}",
                customerId,
                request.tableId(),
                request.bookingDate(),
                request.bookingTime(),
                request.guests()
        );

        RestaurantTable table =
                tableRepository.findById(request.tableId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Table not found with id: "
                                                + request.tableId()
                                )
                        );

        if (!Boolean.TRUE.equals(table.getActive())) {

            throw new RuntimeException("Table is inactive");
        }

        if (request.guests() > table.getCapacity()) {

            throw new RuntimeException(
                    "Guest count exceeds table capacity"
            );
        }

        boolean alreadyBooked =
                bookingRepository
                        .existsByTableIdAndBookingDateAndBookingTimeAndStatus(
                                request.tableId(),
                                request.bookingDate(),
                                request.bookingTime(),
                                BookingStatus.CONFIRMED
                        );

        if (alreadyBooked) {

            throw new RuntimeException(
                    "Table is already booked for this time"
            );
        }

        if (table.getStatus() != TableStatus.AVAILABLE) {

            throw new RuntimeException(
                    "Table is currently " + table.getStatus()
            );
        }

        TableBooking booking =
                TableBooking.builder()
                        .customerId(customerId)
                        .tableId(request.tableId())
                        .bookingDate(request.bookingDate())
                        .bookingTime(request.bookingTime())
                        .guests(request.guests())
                        .status(BookingStatus.CONFIRMED)
                        .build();

        TableBooking savedBooking =
                bookingRepository.save(booking);

        table.setStatus(TableStatus.RESERVED);

        tableRepository.save(table);

        log.info(
                "Booking created successfully | bookingId={} | customerId={} | tableId={}",
                savedBooking.getId(),
                customerId,
                savedBooking.getTableId()
        );

        return mapToResponse(savedBooking);
    }

    // =========================
    // CUSTOMER BOOKINGS
    // =========================

    public List<TableBookingResponse> getMyBookings(
            Long customerId
    ) {

        log.info(
                "Fetching bookings for customer. customerId={}",
                customerId
        );

        List<TableBookingResponse> bookings =
                bookingRepository
                        .findByCustomerId(customerId)
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        log.info(
                "Customer bookings fetched successfully. customerId={}, bookingCount={}",
                customerId,
                bookings.size()
        );

        return bookings;
    }


    // =========================
    // GET BOOKING BY ID
    // =========================

    public TableBookingResponse getBookingById(
            Long bookingId
    ) {

        log.info(
                "Fetching booking by id. bookingId={}",
                bookingId
        );

        TableBooking booking =
                bookingRepository.findById(bookingId)
                        .orElseThrow(() -> {

                            log.warn(
                                    "Booking not found. bookingId={}",
                                    bookingId
                            );

                            return new RuntimeException(
                                    "Booking not found with id: "
                                            + bookingId
                            );
                        });

        log.debug(
                "Booking found. bookingId={}, customerId={}, tableId={}, status={}",
                booking.getId(),
                booking.getCustomerId(),
                booking.getTableId(),
                booking.getStatus()
        );

        return mapToResponse(booking);
    }


    // =========================
    // RESPONSE MAPPER
    // =========================

    private TableBookingResponse mapToResponse(
            TableBooking booking
    ) {

        return new TableBookingResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getTableId(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getGuests(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}