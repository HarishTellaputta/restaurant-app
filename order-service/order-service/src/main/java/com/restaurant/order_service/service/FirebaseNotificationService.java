package com.restaurant.order_service.service;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FirebaseNotificationService {

    public String sendNotification(
            String token,
            String title,
            String body
    ) {

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance()
                    .send(message);

            System.out.println("FCM sent successfully: " + response);

            return response;

        } catch (Exception e) {
            System.out.println("FCM sending failed: " + e.getMessage());
            throw new RuntimeException("Failed to send FCM notification", e);
        }
    }
}