package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.SaveTokenRequest;
import com.restaurant.order_service.service.DeviceTokenService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
public class DeviceTokenController {

    private final DeviceTokenService service;

    public DeviceTokenController(DeviceTokenService service) {
        this.service = service;
    }

    @PostMapping("/token")
    public String saveToken(
            @RequestBody SaveTokenRequest request) {

        service.saveToken(request);

        return "Token saved successfully";
    }
}