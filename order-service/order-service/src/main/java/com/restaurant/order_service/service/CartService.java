package com.restaurant.order_service.service;

import com.restaurant.order_service.client.MenuServiceClient;
import com.restaurant.order_service.dto.*;
import com.restaurant.order_service.entity.Cart;
import com.restaurant.order_service.entity.CartItem;
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
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuServiceClient menuServiceClient;


    // =====================================================
    // GET CART
    // =====================================================

    @Transactional
    public CartResponse getCart(Long customerId) {

        Cart cart = getOrCreateCart(customerId);

        return buildCartResponse(cart);
    }


    // =====================================================
    // ADD ITEM
    // =====================================================

    @Transactional
    public CartResponse addItem(
            Long customerId,
            AddCartItemRequest request
    ) {

        log.info(
                "Adding item to cart | customerId={} | foodItemId={} | quantity={}",
                customerId,
                request.foodItemId(),
                request.quantity()
        );

        Cart cart = getOrCreateCart(customerId);


        // -------------------------------------------------
        // GET FOOD ITEM FROM MENU SERVICE
        // -------------------------------------------------

        FoodItemResponse foodItem =
                menuServiceClient.getFoodItem(
                        request.foodItemId()
                );

        if (foodItem == null) {

            throw new RuntimeException(
                    "Food item not found: "
                            + request.foodItemId()
            );
        }


        // -------------------------------------------------
        // CHECK AVAILABILITY
        // -------------------------------------------------

        if (!foodItem.available()) {

            throw new RuntimeException(
                    "Food item is currently unavailable: "
                            + foodItem.name()
            );
        }


        // -------------------------------------------------
        // CHECK EXISTING ITEM
        // -------------------------------------------------

        CartItem cartItem =
                cartItemRepository
                        .findByCartIdAndFoodItemId(
                                cart.getId(),
                                request.foodItemId()
                        )
                        .orElse(null);


        BigDecimal price =
                BigDecimal.valueOf(foodItem.price());


        if (cartItem != null) {

            // Existing item → increase quantity

            int newQuantity =
                    cartItem.getQuantity()
                            + request.quantity();

            cartItem.setQuantity(newQuantity);

            cartItem.setPrice(price);

            cartItem.setSubtotal(
                    price.multiply(
                            BigDecimal.valueOf(newQuantity)
                    )
            );

        } else {

            // New item

            cartItem =
                    CartItem.builder()
                            .cartId(cart.getId())
                            .foodItemId(foodItem.id())
                            .quantity(request.quantity())
                            .price(price)
                            .subtotal(
                                    price.multiply(
                                            BigDecimal.valueOf(
                                                    request.quantity()
                                            )
                                    )
                            )
                            .build();
        }


        cartItemRepository.save(cartItem);

        recalculateCart(cart);

        return buildCartResponse(cart);
    }


    // =====================================================
    // UPDATE ITEM
    // =====================================================

    @Transactional
    public CartResponse updateItem(
            Long customerId,
            Long itemId,
            UpdateCartItemRequest request
    ) {

        log.info(
                "Updating cart item | customerId={} | itemId={} | quantity={}",
                customerId,
                itemId,
                request.quantity()
        );

        Cart cart = getCartEntity(customerId);


        CartItem cartItem =
                cartItemRepository
                        .findByIdAndCartId(
                                itemId,
                                cart.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cart item not found"
                                )
                        );


        // -------------------------------------------------
        // GET CURRENT FOOD PRICE
        // -------------------------------------------------

        FoodItemResponse foodItem =
                menuServiceClient.getFoodItem(
                        cartItem.getFoodItemId()
                );


        if (foodItem == null) {

            throw new RuntimeException(
                    "Food item not found: "
                            + cartItem.getFoodItemId()
            );
        }


        if (!foodItem.available()) {

            throw new RuntimeException(
                    "Food item is currently unavailable: "
                            + foodItem.name()
            );
        }


        BigDecimal price =
                BigDecimal.valueOf(foodItem.price());


        cartItem.setQuantity(
                request.quantity()
        );

        cartItem.setPrice(price);

        cartItem.setSubtotal(
                price.multiply(
                        BigDecimal.valueOf(
                                request.quantity()
                        )
                )
        );


        cartItemRepository.save(cartItem);

        recalculateCart(cart);

        return buildCartResponse(cart);
    }


    // =====================================================
    // REMOVE ITEM
    // =====================================================

    @Transactional
    public CartResponse removeItem(
            Long customerId,
            Long itemId
    ) {

        log.info(
                "Removing cart item | customerId={} | itemId={}",
                customerId,
                itemId
        );

        Cart cart = getCartEntity(customerId);


        CartItem cartItem =
                cartItemRepository
                        .findByIdAndCartId(
                                itemId,
                                cart.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cart item not found"
                                )
                        );


        cartItemRepository.delete(cartItem);

        recalculateCart(cart);

        return buildCartResponse(cart);
    }


    // =====================================================
    // CLEAR CART
    // =====================================================

    @Transactional
    public void clearCart(Long customerId) {

        log.info(
                "Clearing cart | customerId={}",
                customerId
        );

        Cart cart =
                cartRepository
                        .findByCustomerId(customerId)
                        .orElse(null);

        if (cart == null) {
            return;
        }


        cartItemRepository.deleteByCartId(
                cart.getId()
        );


        cart.setSubtotal(BigDecimal.ZERO);
        cart.setDiscount(BigDecimal.ZERO);
        cart.setDeliveryCharge(BigDecimal.ZERO);
        cart.setPlatformFee(BigDecimal.ZERO);
        cart.setTax(BigDecimal.ZERO);
        cart.setTotalAmount(BigDecimal.ZERO);

        cartRepository.save(cart);
    }


    // =====================================================
    // GET OR CREATE CART
    // =====================================================

    private Cart getOrCreateCart(Long customerId) {

        return cartRepository
                .findByCustomerId(customerId)
                .orElseGet(() -> {

                    Cart cart =
                            Cart.builder()
                                    .customerId(customerId)
                                    .subtotal(BigDecimal.ZERO)
                                    .discount(BigDecimal.ZERO)
                                    .deliveryCharge(BigDecimal.ZERO)
                                    .platformFee(BigDecimal.ZERO)
                                    .tax(BigDecimal.ZERO)
                                    .totalAmount(BigDecimal.ZERO)
                                    .build();

                    return cartRepository.save(cart);
                });
    }


    // =====================================================
    // GET CART
    // =====================================================

    private Cart getCartEntity(Long customerId) {

        return cartRepository
                .findByCustomerId(customerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart not found"
                        )
                );
    }


    // =====================================================
    // RECALCULATE CART
    // =====================================================

    private void recalculateCart(Cart cart) {

        List<CartItem> items =
                cartItemRepository
                        .findByCartId(cart.getId());


        BigDecimal subtotal =
                items.stream()
                        .map(CartItem::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        cart.setSubtotal(subtotal);


        /*
         * For now these are ZERO.
         *
         * We will calculate them during checkout.
         */

        BigDecimal discount =
                BigDecimal.ZERO;

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;

        BigDecimal platformFee =
                BigDecimal.ZERO;

        BigDecimal tax =
                BigDecimal.ZERO;


        BigDecimal total =
                subtotal
                        .subtract(discount)
                        .add(deliveryCharge)
                        .add(platformFee)
                        .add(tax);


        cart.setDiscount(discount);
        cart.setDeliveryCharge(deliveryCharge);
        cart.setPlatformFee(platformFee);
        cart.setTax(tax);
        cart.setTotalAmount(total);


        cartRepository.save(cart);
    }


    // =====================================================
    // BUILD RESPONSE
    // =====================================================

    private CartResponse buildCartResponse(
            Cart cart
    ) {

        List<CartItemResponse> items =
                cartItemRepository
                        .findByCartId(cart.getId())
                        .stream()
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


        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                items,
                cart.getSubtotal(),
                cart.getDiscount(),
                cart.getDeliveryCharge(),
                cart.getPlatformFee(),
                cart.getTax(),
                cart.getTotalAmount()
        );
    }
}