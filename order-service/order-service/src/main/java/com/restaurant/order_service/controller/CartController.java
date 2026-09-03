package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.AddCartItemRequest;
import com.restaurant.order_service.dto.CartResponse;
import com.restaurant.order_service.dto.UpdateCartItemRequest;
import com.restaurant.order_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                cartService.getCart(customerId)
        );
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddCartItemRequest request) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                cartService.addItem(customerId, request)
        );
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long itemId,
            Authentication authentication,
            @Valid @RequestBody UpdateCartItemRequest request) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                cartService.updateItem(
                        customerId,
                        itemId,
                        request
                )
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long itemId,
            Authentication authentication) {

        Long customerId =
                (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                cartService.removeItem(
                        customerId,
                        itemId
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            Authentication authentication) {

        Long customerId =
                (Long) authentication.getPrincipal();

        cartService.clearCart(customerId);

        return ResponseEntity.noContent().build();
    }
}