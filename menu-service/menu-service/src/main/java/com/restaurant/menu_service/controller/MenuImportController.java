package com.restaurant.menu_service.controller;

import com.restaurant.menu_service.dto.MenuImportResponse;
import com.restaurant.menu_service.service.MenuImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuImportController {

    private final MenuImportService menuImportService;

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public MenuImportResponse importMenu(
            @RequestParam("file") MultipartFile file
    ) {

        return menuImportService.importMenu(file);
    }
}