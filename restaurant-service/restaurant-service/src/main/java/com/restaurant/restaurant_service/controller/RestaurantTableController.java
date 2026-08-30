package com.restaurant.restaurant_service.controller;

import com.restaurant.restaurant_service.dto.RestaurantTableRequest;
import com.restaurant.restaurant_service.dto.RestaurantTableResponse;
import com.restaurant.restaurant_service.dto.TableStatusRequest;
import com.restaurant.restaurant_service.service.RestaurantTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class RestaurantTableController {

    private final RestaurantTableService tableService;


    // =========================
    // ADMIN - CREATE TABLE
    // =========================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantTableResponse createTable(
            @Valid @RequestBody RestaurantTableRequest request
    ) {

        return tableService.createTable(request);
    }


    // =========================
    // GET ALL TABLES
    // =========================

    @GetMapping
    public List<RestaurantTableResponse> getAllTables() {

        return tableService.getAllTables();
    }


    // =========================
    // CUSTOMER - AVAILABLE TABLES
    // =========================

    @GetMapping("/available")
    public List<RestaurantTableResponse> getAvailableTables() {

        return tableService.getAvailableTables();
    }


    // =========================
    // ADMIN / STAFF
    // UPDATE TABLE STATUS
    // =========================

    @PutMapping("/{id}/status")
    public RestaurantTableResponse updateTableStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TableStatusRequest request
    ) {


        System.out.println("========== UPDATE TABLE STATUS ==========");
        System.out.println("TABLE ID: " + id);
        System.out.println("STATUS: " + request.status());
        return tableService.updateTableStatus(
                id,
                request
        );
    }
}