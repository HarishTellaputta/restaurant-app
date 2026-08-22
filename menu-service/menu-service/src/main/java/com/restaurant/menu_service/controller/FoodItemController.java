package com.restaurant.menu_service.controller;


import com.restaurant.menu_service.dto.FoodItemRequest;
import com.restaurant.menu_service.dto.FoodItemResponse;
import com.restaurant.menu_service.entity.FoodItem;
import com.restaurant.menu_service.service.FoodItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/food-items")
@RequiredArgsConstructor
public class FoodItemController {

    private final FoodItemService foodItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodItemResponse createFoodItem(
            @Valid @RequestBody FoodItemRequest request) {

        return foodItemService.createFoodItem(request);
    }

    @GetMapping
    public List<FoodItemResponse> getAllFoodItems() {

        return foodItemService.getAllFoodItems();
    }

    @GetMapping("/{id}")
    public FoodItemResponse getFoodItemById(
            @PathVariable Long id) {

        return foodItemService.getFoodItemById(id);
    }
    @PutMapping("/{id}")
    public FoodItemResponse updateFoodItem(
            @PathVariable Long id,
          @Valid @RequestBody FoodItemRequest request
    ) {

        return foodItemService.updateFoodItem(
                id,request

        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFoodItem(@PathVariable Long id) {

        foodItemService.deleteFoodItem(id);
    }

    @GetMapping("/category/{categoryId}")
    public List<FoodItemResponse> getFoodItemsByCategory(
            @PathVariable Long categoryId) {

        return foodItemService.getFoodItemsByCategory(categoryId);
    }

    @PostMapping(
            value = "/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FoodItemResponse uploadFoodItemImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {

        return foodItemService.uploadFoodItemImage(
                id,
                file
        );
    }
}