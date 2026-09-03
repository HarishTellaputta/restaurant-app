package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.CheckoutRequest;
import com.restaurant.order_service.dto.CheckoutResponse;
import com.restaurant.order_service.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;


    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                checkoutService.checkout(
                        customerId,
                        request
                )
        );
    }
}