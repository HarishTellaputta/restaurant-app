package com.restaurant.order_service.controller;

import com.restaurant.order_service.service.FirebaseNotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/fcm")
public class FirebaseNotificationController {

    private final FirebaseNotificationService firebaseNotificationService;

    public FirebaseNotificationController(
            FirebaseNotificationService firebaseNotificationService) {
        this.firebaseNotificationService = firebaseNotificationService;
    }

    @PostMapping("/send")
    public String sendNotification(
            @RequestParam String token,
            @RequestParam String title,
            @RequestParam String body
    ) {
        return firebaseNotificationService.sendNotification(
                token,
                title,
                body
        );
    }
}