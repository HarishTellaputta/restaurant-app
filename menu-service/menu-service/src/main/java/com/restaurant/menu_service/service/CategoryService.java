package com.restaurant.menu_service.service;

import com.restaurant.menu_service.dto.CategoryRequest;
import com.restaurant.menu_service.dto.CategoryResponse;
import com.restaurant.menu_service.entity.Category;
import com.restaurant.menu_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // CREATE
    public CategoryResponse createCategory(CategoryRequest request) {

        if (categoryRepository.existsByName(request.name())) {
            throw new RuntimeException("Category already exists");
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .active(
                        request.active() != null
                                ? request.active()
                                : true
                )
                .build();

        Category savedCategory = categoryRepository.save(category);

        return mapToResponse(savedCategory);
    }

    // GET ALL
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET BY ID
    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id: " + id
                        )
                );

        return mapToResponse(category);
    }

    // UPDATE
    public CategoryResponse updateCategory(
            Long id,
            CategoryRequest request
    ) {

        Category existingCategory =
                categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found with id: " + id
                                )
                        );

        existingCategory.setName(request.name());
        existingCategory.setDescription(request.description());

        if (request.active() != null) {
            existingCategory.setActive(request.active());
        }

        Category updatedCategory =
                categoryRepository.save(existingCategory);

        return mapToResponse(updatedCategory);
    }

    // DELETE
    public void deleteCategory(Long id) {

        Category category =
                categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found with id: " + id
                                )
                        );

        categoryRepository.delete(category);
    }

    // ENTITY → DTO
    private CategoryResponse mapToResponse(Category category) {

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive()
        );
    }
}