package com.restaurant.menu_service.service;

import com.restaurant.menu_service.dto.MenuExcelRow;
import com.restaurant.menu_service.dto.MenuImportResponse;
import com.restaurant.menu_service.entity.Category;
import com.restaurant.menu_service.entity.FoodItem;
import com.restaurant.menu_service.repository.CategoryRepository;
import com.restaurant.menu_service.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuImportService {

    private final CategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;

    @Transactional
    public MenuImportResponse importMenu(MultipartFile file) {

        log.info(
                "Menu import started. File name: {}, size: {} bytes",
                file.getOriginalFilename(),
                file.getSize()
        );

        List<MenuExcelRow> rows = readExcel(file);

        log.info(
                "Excel file read successfully. Total rows: {}",
                rows.size()
        );

        List<String> errors = validateRows(rows);

        if (!errors.isEmpty()) {

            log.warn(
                    "Menu import validation failed. Total errors: {}",
                    errors.size()
            );

            errors.forEach(error ->
                    log.warn("Import validation error: {}", error)
            );

            return new MenuImportResponse(
                    0,
                    0,
                    0,
                    0,
                    errors.size(),
                    errors
            );
        }

        int categoriesCreated = 0;
        int categoriesUpdated = 0;

        int foodItemsCreated = 0;
        int foodItemsUpdated = 0;

        // ==========================================
        // STEP 1: CREATE / UPDATE CATEGORIES
        // ==========================================

        log.info("Starting category import...");

        Map<String, Category> categoryMap =
                new HashMap<>();

        Set<String> uniqueCategories =
                new LinkedHashSet<>();

        for (MenuExcelRow row : rows) {

            uniqueCategories.add(
                    row.categoryName().trim()
            );
        }

        log.info(
                "Unique categories found in Excel: {}",
                uniqueCategories.size()
        );

        for (String categoryName : uniqueCategories) {

            MenuExcelRow row = rows.stream()
                    .filter(r ->
                            r.categoryName()
                                    .trim()
                                    .equalsIgnoreCase(categoryName)
                    )
                    .findFirst()
                    .orElseThrow();

            Optional<Category> existing =
                    categoryRepository.findByNameIgnoreCase(
                            categoryName
                    );

            Category category;

            if (existing.isPresent()) {

                category = existing.get();

                category.setDescription(
                        row.categoryDescription()
                );

                category.setActive(
                        row.categoryActive() != null
                                ? row.categoryActive()
                                : true
                );

                categoryRepository.save(category);

                categoriesUpdated++;

                log.info(
                        "Category updated. ID: {}, name: {}",
                        category.getId(),
                        category.getName()
                );

            } else {

                category = Category.builder()
                        .name(categoryName)
                        .description(
                                row.categoryDescription()
                        )
                        .active(
                                row.categoryActive() != null
                                        ? row.categoryActive()
                                        : true
                        )
                        .build();

                category =
                        categoryRepository.save(category);

                categoriesCreated++;

                log.info(
                        "Category created. ID: {}, name: {}",
                        category.getId(),
                        category.getName()
                );
            }

            categoryMap.put(
                    categoryName.toLowerCase(),
                    category
            );
        }

        log.info(
                "Category import completed. Created: {}, Updated: {}",
                categoriesCreated,
                categoriesUpdated
        );

        // ==========================================
        // STEP 2: CREATE / UPDATE FOOD ITEMS
        // ==========================================

        log.info("Starting food item import...");

        for (MenuExcelRow row : rows) {

            Category category =
                    categoryMap.get(
                            row.categoryName()
                                    .trim()
                                    .toLowerCase()
                    );

            if (category == null) {

                log.error(
                        "Category not found in category map. Category: {}",
                        row.categoryName()
                );

                throw new RuntimeException(
                        "Category not found: "
                                + row.categoryName()
                );
            }

            FoodItem foodItem;

            if (row.foodItemId() != null) {

                foodItem =
                        foodItemRepository
                                .findById(row.foodItemId())
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Food item not found with id: "
                                                        + row.foodItemId()
                                        )
                                );

                foodItemsUpdated++;

                log.info(
                        "Updating food item. ID: {}, name: {}",
                        foodItem.getId(),
                        row.foodItemName()
                );

            } else {

                foodItem = new FoodItem();

                foodItemsCreated++;

                log.info(
                        "Creating new food item. Name: {}",
                        row.foodItemName()
                );
            }

            foodItem.setName(
                    row.foodItemName()
            );

            foodItem.setDescription(
                    row.foodItemDescription()
            );

            foodItem.setPrice(
                    row.price()
            );

            foodItem.setImageUrl(
                    row.imageUrl()
            );

            foodItem.setAvailable(
                    row.available() != null
                            ? row.available()
                            : true
            );

            foodItem.setCategory(category);

            foodItemRepository.save(foodItem);
        }

        log.info(
                "Food item import completed. Created: {}, Updated: {}",
                foodItemsCreated,
                foodItemsUpdated
        );

        log.info(
                "Menu import completed successfully. " +
                        "Categories Created: {}, Categories Updated: {}, " +
                        "Food Items Created: {}, Food Items Updated: {}",
                categoriesCreated,
                categoriesUpdated,
                foodItemsCreated,
                foodItemsUpdated
        );

        return new MenuImportResponse(
                categoriesCreated,
                categoriesUpdated,
                foodItemsCreated,
                foodItemsUpdated,
                0,
                List.of()
        );
    }

    // ==========================================
    // READ EXCEL
    // ==========================================

    private List<MenuExcelRow> readExcel(
            MultipartFile file
    ) {

        log.debug(
                "Reading Excel file: {}",
                file.getOriginalFilename()
        );

        List<MenuExcelRow> rows =
                new ArrayList<>();

        try (
                Workbook workbook =
                        WorkbookFactory.create(
                                file.getInputStream()
                        )
        ) {

            Sheet sheet =
                    workbook.getSheetAt(0);

            log.debug(
                    "Excel sheet loaded. Sheet name: {}",
                    sheet.getSheetName()
            );

            boolean firstRow = true;

            for (Row row : sheet) {

                if (firstRow) {
                    firstRow = false;
                    continue;
                }

                if (row.getCell(0) == null ||
                        getString(row.getCell(0)) == null ||
                        getString(row.getCell(0)).equalsIgnoreCase("categoryName")) {

                    continue;
                }

                rows.add(
                        new MenuExcelRow(

                                getString(row.getCell(0)),

                                getString(row.getCell(1)),

                                getBoolean(row.getCell(2)),

                                getLong(row.getCell(3)),

                                getString(row.getCell(4)),

                                getString(row.getCell(5)),

                                getDouble(row.getCell(6)),

                                getString(row.getCell(7)),

                                getBoolean(row.getCell(8))
                        )
                );
            }

        } catch (IOException e) {

            log.error(
                    "Failed to read Excel file: {}",
                    file.getOriginalFilename(),
                    e
            );

            throw new RuntimeException(
                    "Failed to read Excel file",
                    e
            );
        }

        return rows;
    }

    // ==========================================
    // VALIDATION
    // ==========================================

    private List<String> validateRows(
            List<MenuExcelRow> rows
    ) {

        log.debug(
                "Starting Excel row validation. Rows: {}",
                rows.size()
        );

        List<String> errors =
                new ArrayList<>();

        int rowNumber = 2;

        for (MenuExcelRow row : rows) {

            if (row.categoryName() == null ||
                    row.categoryName().isBlank()) {

                errors.add(
                        "Row " + rowNumber +
                                ": categoryName is required"
                );
            }

            if (row.foodItemName() == null ||
                    row.foodItemName().isBlank()) {

                errors.add(
                        "Row " + rowNumber +
                                ": foodItemName is required"
                );
            }

            if (row.price() == null ||
                    row.price() <= 0) {

                errors.add(
                        "Row " + rowNumber +
                                ": price must be greater than 0"
                );
            }

            rowNumber++;
        }

        log.debug(
                "Excel validation completed. Errors: {}",
                errors.size()
        );

        return errors;
    }

    // ==========================================
    // EXCEL HELPERS
    // ==========================================

    private String getString(Cell cell) {

        if (cell == null) {
            return null;
        }

        return cell.toString().trim();
    }
    private Boolean getBoolean(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }

        if (cell.getCellType() == CellType.NUMERIC) {

            double value = cell.getNumericCellValue();

            if (value == 1) {
                return true;
            }

            if (value == 0) {
                return false;
            }

            throw new RuntimeException(
                    "Invalid boolean numeric value: " + value
            );
        }

        if (cell.getCellType() == CellType.STRING) {

            String value =
                    cell.getStringCellValue()
                            .trim()
                            .toLowerCase();

            if (value.isEmpty()) {
                return null;
            }

            switch (value) {

                case "true":
                case "yes":
                case "y":
                case "1":
                    return true;

                case "false":
                case "no":
                case "n":
                case "0":
                    return false;

                default:
                    throw new RuntimeException(
                            "Invalid boolean value in Excel: " + value
                    );
            }
        }

        throw new RuntimeException(
                "Unsupported boolean cell type: "
                        + cell.getCellType()
        );
    }
    private Long getLong(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }

        if (cell.getCellType() == CellType.STRING) {

            String value = cell.getStringCellValue().trim();

            if (value.isEmpty()) {
                return null;
            }

            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {

                throw new RuntimeException(
                        "Invalid foodItemId value: " + value
                );
            }
        }

        throw new RuntimeException(
                "Invalid foodItemId cell type: "
                        + cell.getCellType()
        );
    }
    private Double getDouble(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        if (cell.getCellType() == CellType.STRING) {

            String value = cell.getStringCellValue().trim();

            if (value.isEmpty()) {
                return null;
            }

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {

                throw new RuntimeException(
                        "Invalid price value: " + value
                );
            }
        }

        throw new RuntimeException(
                "Invalid price cell type: "
                        + cell.getCellType()
        );
    }
}