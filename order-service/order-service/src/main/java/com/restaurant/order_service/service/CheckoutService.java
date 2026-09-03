package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.dto.*;
import com.restaurant.order_service.entity.Cart;
import com.restaurant.order_service.entity.CartItem;
import com.restaurant.order_service.entity.OrderType;
import com.restaurant.order_service.repository.CartItemRepository;
import com.restaurant.order_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final DeliveryChargeService deliveryChargeService;
    private final MenuServiceClient menuServiceClient;


    // =====================================================
    // CALCULATE CHECKOUT
    // =====================================================

    @Transactional(readOnly = true)
    public CheckoutResponse checkout(
            Long customerId,
            CheckoutRequest request
    ) {

        log.info(
                "Checkout started | customerId={} | orderType={}",
                customerId,
                request.orderType()
        );


        // -------------------------------------------------
        // GET CART
        // -------------------------------------------------

        Cart cart =
                cartRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cart not found"
                                )
                        );


        // -------------------------------------------------
        // GET CART ITEMS
        // -------------------------------------------------

        List<CartItem> cartItems =
                cartItemRepository
                        .findByCartId(cart.getId());


        if (cartItems.isEmpty()) {

            throw new RuntimeException(
                    "Cart is empty"
            );
        }


        // -------------------------------------------------
        // SUBTOTAL
        // -------------------------------------------------

        BigDecimal subtotal =
                cartItems.stream()
                        .map(CartItem::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        // -------------------------------------------------
        // DELIVERY CHARGE
        // -------------------------------------------------

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;


        if (request.orderType() == OrderType.DELIVERY) {

            deliveryCharge =
                    deliveryChargeService
                            .calculateDeliveryCharge(
                                    request.distanceKm(),
                                    request.peakTime(),
                                    request.weather()
                            );
        }


        // -------------------------------------------------
        // DISCOUNT
        // -------------------------------------------------

        BigDecimal discount =
                BigDecimal.ZERO;

        /*
         * Coupon / offer calculation will be added here.
         */


        // -------------------------------------------------
        // PLATFORM FEE
        // -------------------------------------------------

        BigDecimal platformFee =
                BigDecimal.ZERO;

        /*
         * Platform fee calculation will be added here.
         */


        // -------------------------------------------------
        // TAX
        // -------------------------------------------------

        BigDecimal tax =
                BigDecimal.ZERO;

        /*
         * Tax calculation will be added here.
         */


        // -------------------------------------------------
        // FINAL TOTAL
        // -------------------------------------------------

        BigDecimal totalAmount =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee)
                        .add(tax);


        log.info(
                "Checkout calculated | customerId={} | subtotal={} | delivery={} | total={}",
                customerId,
                subtotal,
                deliveryCharge,
                totalAmount
        );


        // -------------------------------------------------
        // BUILD RESPONSE
        // -------------------------------------------------
        List<CartItemResponse> items =
                cartItems.stream()
                        .map(item -> {

                            String foodItemName = null;
                            String imageUrl = null;

                            try {

                                FoodItemResponse foodItem =
                                        menuServiceClient.getFoodItem(
                                                item.getFoodItemId()
                                        );

                                if (foodItem != null) {

                                    foodItemName =
                                            foodItem.name();

                                    imageUrl =
                                            foodItem.imageUrl();
                                }

                            } catch (Exception e) {

                                log.warn(
                                        "Unable to fetch food item details | foodItemId={}",
                                        item.getFoodItemId()
                                );
                            }

                            return new CartItemResponse(
                                    item.getId(),
                                    item.getFoodItemId(),
                                    foodItemName,
                                    imageUrl,
                                    item.getQuantity(),
                                    item.getPrice(),
                                    item.getSubtotal()
                            );
                        })
                        .toList();


        return new CheckoutResponse(
                cart.getId(),
                customerId,
                request.orderType(),
                request.paymentMethod(),
                items,
                subtotal,
                discount,
                deliveryCharge,
                platformFee,
                tax,
                totalAmount
        );
    }
}