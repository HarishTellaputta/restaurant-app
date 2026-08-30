package com.restaurant.restaurant_service.service;

import com.restaurant.restaurant_service.client.OrderClient;
import com.restaurant.restaurant_service.dto.OrderResponse;
import com.restaurant.restaurant_service.dto.RestaurantTableRequest;
import com.restaurant.restaurant_service.dto.RestaurantTableResponse;
import com.restaurant.restaurant_service.dto.TableStatusRequest;
import com.restaurant.restaurant_service.entity.BookingStatus;
import com.restaurant.restaurant_service.entity.RestaurantTable;
import com.restaurant.restaurant_service.entity.TableStatus;
import com.restaurant.restaurant_service.repository.RestaurantTableRepository;
import com.restaurant.restaurant_service.repository.TableBookingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final TableBookingRepository tableBookingRepository;
    private final OrderClient orderClient;


    // =========================
    // CREATE TABLE
    // =========================

    public RestaurantTableResponse createTable(
            RestaurantTableRequest request
    ) {

        log.info(
                "Creating restaurant table. tableNumber={}, capacity={}, active={}",
                request.tableNumber(),
                request.capacity(),
                request.active()
        );

        if (tableRepository.existsByTableNumber(
                request.tableNumber()
        )) {

            log.warn(
                    "Table creation failed. Table number already exists: {}",
                    request.tableNumber()
            );

            throw new RuntimeException(
                    "Table number already exists"
            );
        }

        RestaurantTable table = RestaurantTable.builder()
                .tableNumber(request.tableNumber())
                .capacity(request.capacity())
                .status(TableStatus.AVAILABLE)
                .active(
                        request.active() != null
                                ? request.active()
                                : true
                )
                .build();

        RestaurantTable savedTable =
                tableRepository.save(table);

        log.info(
                "Restaurant table created successfully. id={}, tableNumber={}",
                savedTable.getId(),
                savedTable.getTableNumber()
        );

        return mapToResponse(savedTable);
    }


    // =========================
    // GET ALL TABLES
    // =========================

    public List<RestaurantTableResponse> getAllTables() {

        log.info("Fetching all restaurant tables");

        List<RestaurantTableResponse> tables =
                tableRepository.findAll()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        log.info(
                "Fetched {} restaurant tables",
                tables.size()
        );

        return tables;
    }


    // =========================
    // GET AVAILABLE TABLES
    // =========================

    public List<RestaurantTableResponse> getAvailableTables() {

        log.info("Fetching available restaurant tables");

        List<RestaurantTableResponse> tables =
                tableRepository
                        .findByStatusAndActiveTrue(
                                TableStatus.AVAILABLE
                        )
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        log.info(
                "Available restaurant tables count: {}",
                tables.size()
        );

        return tables;
    }


    // =========================
    // UPDATE TABLE STATUS
    // =========================

    @Transactional
    public RestaurantTableResponse updateTableStatus(
            Long id,
            TableStatusRequest request
    ) {

        log.info(
                "========== UPDATE TABLE STATUS START =========="
        );

        log.info(
                "Updating table status. tableId={}, requestedStatus={}",
                id,
                request.status()
        );


        // ============================================
        // FIND TABLE
        // ============================================

        RestaurantTable table =
                tableRepository.findById(id)
                        .orElseThrow(() -> {

                            log.error(
                                    "Table not found. tableId={}",
                                    id
                            );

                            return new RuntimeException(
                                    "Table not found with id: " + id
                            );
                        });

        log.info(
                "Table found. tableId={}, tableNumber={}, currentStatus={}",
                table.getId(),
                table.getTableNumber(),
                table.getStatus()
        );


        // ============================================
        // UPDATE TABLE STATUS
        // ============================================

        TableStatus oldStatus = table.getStatus();

        table.setStatus(request.status());

        log.info(
                "Table status changed. tableId={}, oldStatus={}, newStatus={}",
                id,
                oldStatus,
                request.status()
        );


        // ============================================
        // TABLE → AVAILABLE
        // CANCEL BOOKING + RELATED ORDER
        // ============================================

        if (request.status() == TableStatus.AVAILABLE) {

            log.info(
                    "Table {} changed to AVAILABLE. Checking confirmed booking...",
                    id
            );

            tableBookingRepository
                    .findFirstByTableIdAndStatusOrderByCreatedAtDesc(
                            id,
                            BookingStatus.CONFIRMED
                    )
                    .ifPresentOrElse(
                            booking -> {

                                log.info(
                                        "Confirmed booking found. " +
                                                "bookingId={}, tableId={}",
                                        booking.getId(),
                                        booking.getTableId()
                                );


                                // =========================================
                                // 1. CANCEL BOOKING
                                // =========================================

                                booking.setStatus(
                                        BookingStatus.CANCELLED
                                );

                                tableBookingRepository.save(booking);

                                log.info(
                                        "Booking cancelled successfully. bookingId={}",
                                        booking.getId()
                                );


                                // =========================================
                                // 2. FIND RELATED ORDER
                                // =========================================

                                try {

                                    OrderResponse order =
                                            orderClient.getOrderByBookingId(
                                                    booking.getId(),
                                                    booking.getCustomerId(),
                                                    booking.getTableId()
                                            );

                                    if (order == null) {

                                        log.info(
                                                "No related order found. bookingId={}",
                                                booking.getId()
                                        );

                                        return;
                                    }


                                    log.info(
                                            "Related order found. " +
                                                    "orderId={}, bookingId={}",
                                            order.id(),
                                            booking.getId()
                                    );


                                    // =====================================
                                    // 3. CANCEL ORDER
                                    // =====================================

                                    orderClient.cancelOrder(
                                            order.id()
                                    );

                                    log.info(
                                            "Related order cancellation request " +
                                                    "completed. orderId={}, bookingId={}",
                                            order.id(),
                                            booking.getId()
                                    );

                                } catch (Exception e) {

                                    log.error(
                                            "Failed to cancel related order. " +
                                                    "bookingId={}, customerId={}, tableId={}",
                                            booking.getId(),
                                            booking.getCustomerId(),
                                            booking.getTableId(),
                                            e
                                    );

                                    throw new RuntimeException(
                                            "Booking cancelled but failed to cancel related order",
                                            e
                                    );
                                }
                            },

                            () -> log.info(
                                    "No confirmed booking found for table {}",
                                    id
                            )
                    );

        } else {

            log.info(
                    "Table status is {}. Booking cancellation logic skipped. tableId={}",
                    request.status(),
                    id
            );
        }


        // ============================================
        // SAVE TABLE
        // ============================================

        RestaurantTable updatedTable =
                tableRepository.save(table);

        log.info(
                "Table saved successfully. tableId={}, tableNumber={}, finalStatus={}",
                updatedTable.getId(),
                updatedTable.getTableNumber(),
                updatedTable.getStatus()
        );


        RestaurantTableResponse response =
                mapToResponse(updatedTable);

        log.info(
                "========== UPDATE TABLE STATUS END =========="
        );

        return response;
    }


    // =========================
    // ENTITY → RESPONSE DTO
    // =========================

    private RestaurantTableResponse mapToResponse(
            RestaurantTable table
    ) {

        return new RestaurantTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getStatus(),
                table.getActive()
        );
    }
}