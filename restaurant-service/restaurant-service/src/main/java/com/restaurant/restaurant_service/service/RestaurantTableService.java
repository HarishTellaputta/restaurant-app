package com.restaurant.restaurant_service.service;

import com.restaurant.restaurant_service.dto.RestaurantTableRequest;
import com.restaurant.restaurant_service.dto.RestaurantTableResponse;
import com.restaurant.restaurant_service.dto.TableStatusRequest;
import com.restaurant.restaurant_service.entity.RestaurantTable;
import com.restaurant.restaurant_service.entity.TableStatus;
import com.restaurant.restaurant_service.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;


    // =========================
    // CREATE TABLE
    // =========================

    public RestaurantTableResponse createTable(
            RestaurantTableRequest request
    ) {

        if (tableRepository.existsByTableNumber(
                request.tableNumber()
        )) {
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

        return mapToResponse(savedTable);
    }


    // =========================
    // GET ALL TABLES
    // =========================

    public List<RestaurantTableResponse> getAllTables() {

        return tableRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================
    // GET AVAILABLE TABLES
    // =========================

    public List<RestaurantTableResponse> getAvailableTables() {

        return tableRepository
                .findByStatusAndActiveTrue(TableStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================
    // UPDATE TABLE STATUS
    // =========================

    public RestaurantTableResponse updateTableStatus(
            Long id,
            TableStatusRequest request
    ) {

        RestaurantTable table =
                tableRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Table not found with id: " + id
                                )
                        );

        table.setStatus(request.status());

        RestaurantTable updatedTable =
                tableRepository.save(table);

        return mapToResponse(updatedTable);
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