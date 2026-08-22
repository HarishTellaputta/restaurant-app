package com.restaurant.order_service.client;

import com.restaurant.order_service.dto.FoodItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "menu-service",
        url = "${menu-service.url}"
)
public interface MenuServiceClient {

    @GetMapping("/api/food-items/{id}")
    FoodItemResponse getFoodItem(
            @PathVariable("id") Long foodItemId
    );
}