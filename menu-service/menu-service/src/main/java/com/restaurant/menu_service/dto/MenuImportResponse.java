package com.restaurant.menu_service.dto;

import java.util.List;

public record MenuImportResponse(

        int categoriesCreated,
        int categoriesUpdated,

        int foodItemsCreated,
        int foodItemsUpdated,

        int failedRows,

        List<String> errors

) {
}