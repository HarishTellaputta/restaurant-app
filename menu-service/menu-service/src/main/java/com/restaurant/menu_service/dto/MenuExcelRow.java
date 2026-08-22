package com.restaurant.menu_service.dto;

public record MenuExcelRow(

        String categoryName,
        String categoryDescription,
        Boolean categoryActive,

        Long foodItemId,
        String foodItemName,
        String foodItemDescription,
        Double price,
        String imageUrl,
        Boolean available

) {
}