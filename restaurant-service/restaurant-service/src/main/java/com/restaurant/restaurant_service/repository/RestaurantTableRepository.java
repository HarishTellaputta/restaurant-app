package com.restaurant.restaurant_service.repository;

import com.restaurant.restaurant_service.entity.RestaurantTable;
import com.restaurant.restaurant_service.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantTableRepository
        extends JpaRepository<RestaurantTable, Long> {

    boolean existsByTableNumber(Integer tableNumber);

    List<RestaurantTable> findByStatusAndActiveTrue(
            TableStatus status
    );
}