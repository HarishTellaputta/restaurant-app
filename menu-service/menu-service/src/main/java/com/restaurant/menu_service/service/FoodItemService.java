package com.restaurant.menu_service.service;

import com.restaurant.menu_service.dto.FoodItemRequest;
import com.restaurant.menu_service.dto.FoodItemResponse;
import com.restaurant.menu_service.entity.Category;
import com.restaurant.menu_service.entity.FoodItem;
import com.restaurant.menu_service.repository.CategoryRepository;
import com.restaurant.menu_service.repository.FoodItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;


    // =========================
    // CREATE
    // =========================

    public FoodItemResponse createFoodItem(
            FoodItemRequest request
    ) {

        Category category = categoryRepository.findById(
                        request.categoryId()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id: "
                                        + request.categoryId()
                        )
                );

        FoodItem foodItem = FoodItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .available(
                        request.available() != null
                                ? request.available()
                                : true
                )
                .category(category)
                .build();

        FoodItem savedFoodItem =
                foodItemRepository.save(foodItem);

        return responseMapper(savedFoodItem);
    }


    // =========================
    // GET ALL
    // =========================

    public List<FoodItemResponse> getAllFoodItems() {

        return foodItemRepository.findAll()
                .stream()
                .map(this::responseMapper)
                .toList();
    }

    public List<FoodItemResponse> searchFoodItems(String keyword) {

        return foodItemRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::responseMapper)
                .toList();
    }


    // =========================
    // GET BY ID
    // =========================

    public FoodItemResponse getFoodItemById(Long id) {

        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Food item not found: " + id)
                );

        return new FoodItemResponse(
                foodItem.getId(),
                foodItem.getName(),
                foodItem.getDescription(),
                foodItem.getPrice(),
                foodItem.getImageUrl(),
                foodItem.getAvailable(),
                foodItem.getCategory().getId(),
                foodItem.getCategory().getName()
        );
    }

    // =========================
    // UPDATE
    // =========================

    public FoodItemResponse updateFoodItem(
            Long id,
            FoodItemRequest request
    ) {

        FoodItem existingFoodItem =
                foodItemRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Food item not found with id: "
                                                + id
                                )
                        );

        Category category =
                categoryRepository.findById(
                                request.categoryId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found with id: "
                                                + request.categoryId()
                                )
                        );

        existingFoodItem.setName(request.name());
        existingFoodItem.setDescription(request.description());
        existingFoodItem.setPrice(request.price());
        existingFoodItem.setImageUrl(request.imageUrl());

        if (request.available() != null) {
            existingFoodItem.setAvailable(
                    request.available()
            );
        }

        existingFoodItem.setCategory(category);

        FoodItem updatedFoodItem =
                foodItemRepository.save(existingFoodItem);

        return responseMapper(updatedFoodItem);
    }


    // =========================
    // DELETE
    // =========================

    @Transactional
    public void deleteFoodItem(Long id) {

        FoodItem foodItem =
                foodItemRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Food item not found with id: "
                                                + id
                                )
                        );


        if (foodItem.getImageUrl() != null &&
                !foodItem.getImageUrl().isBlank()) {

            String fileName =
                    extractFileName(
                            foodItem.getImageUrl()
                    );

            fileStorageService.deleteFile(
                    fileName
            );
        }


        foodItemRepository.delete(foodItem);
    }


    // =========================
    // GET BY CATEGORY
    // =========================

    public List<FoodItemResponse> getFoodItemsByCategory(
            Long categoryId
    ) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException(
                    "Category not found with id: "
                            + categoryId
            );
        }

        return foodItemRepository
                .findByCategoryId(categoryId)
                .stream()
                .map(this::responseMapper)
                .toList();
    }


    // =========================
    // ENTITY → RESPONSE DTO
    // =========================

    private FoodItemResponse responseMapper(
            FoodItem foodItem
    ) {

        return new FoodItemResponse(
                foodItem.getId(),
                foodItem.getName(),
                foodItem.getDescription(),
                foodItem.getPrice(),
                foodItem.getImageUrl(),
                foodItem.getAvailable(),
                foodItem.getCategory().getId(),
                foodItem.getCategory().getName()
        );
    }

    @Transactional
    public FoodItemResponse uploadFoodItemImage(
            Long foodItemId,
            MultipartFile file
    ) {

        FoodItem foodItem =
                foodItemRepository.findById(foodItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Food item not found with id: "
                                                + foodItemId
                                )
                        );


        // Delete old image if one exists
        if (foodItem.getImageUrl() != null &&
                !foodItem.getImageUrl().isBlank()) {

            String oldFileName =
                    extractFileName(
                            foodItem.getImageUrl()
                    );

            fileStorageService.deleteFile(
                    oldFileName
            );
        }


        // Store new image
        String fileName =
                fileStorageService.storeFile(file);


        // Save URL in database
        String imageUrl =
                "/uploads/food-items/" + fileName;


        foodItem.setImageUrl(imageUrl);


        FoodItem savedFoodItem =
                foodItemRepository.save(foodItem);


        return responseMapper(savedFoodItem);
    }

    private String extractFileName(
            String imageUrl
    ) {

        if (imageUrl == null ||
                imageUrl.isBlank()) {

            return null;
        }


        int lastSlash =
                imageUrl.lastIndexOf('/');


        if (lastSlash == -1) {

            return imageUrl;
        }


        return imageUrl.substring(
                lastSlash + 1
        );
    }
}