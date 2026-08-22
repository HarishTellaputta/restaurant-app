package com.restaurant.menu_service.dto;

public record CategoryResponse(

        Long id,

        String name,

        String description,

        Boolean active

) {
}