package com.restaurant.order_service.controller;

import com.restaurant.order_service.entity.Notification;
import com.restaurant.order_service.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(
            @PathVariable Long userId) {

        return notificationService.getUserNotifications(userId);
    }
}