package com.restaurant.menu_service.repository;


import com.restaurant.menu_service.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    boolean existsByName(String name);

    List<FoodItem> findByCategoryId(Long categoryId);
}
