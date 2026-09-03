package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndFoodItemId(
            Long cartId,
            Long foodItemId
    );

    Optional<CartItem> findByIdAndCartId(
            Long id,
            Long cartId
    );

    void deleteByCartId(Long cartId);
}