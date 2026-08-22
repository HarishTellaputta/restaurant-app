package com.restaurant.menu_service.repository;


import com.restaurant.menu_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);
    Optional<Category> findByNameIgnoreCase(String name);
}