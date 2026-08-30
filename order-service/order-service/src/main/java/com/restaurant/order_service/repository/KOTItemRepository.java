package com.restaurant.order_service.repository;

import com.restaurant.order_service.entity.KOTItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KOTItemRepository
        extends JpaRepository<KOTItem, Long> {

    List<KOTItem> findByKotId(Long kotId);

    void deleteByKotId(Long kotId);
    List<KOTItem> findByOrderItemId(Long orderItemId);
}