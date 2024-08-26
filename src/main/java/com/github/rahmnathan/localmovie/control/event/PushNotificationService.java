package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PushNotificationService {
    private static final String MOVIE_TOPIC = "movies";
    private final ServiceConfig serviceConfig;
    private final FirebaseMessaging firebaseApp;

    void sendPushNotifications(String title, String path) {
        if (serviceConfig.isNotificationsEnabled()) {
            log.info("Sending notification of new movie: {} to {} clients", path, MOVIE_TOPIC);

            Message msg = Message.builder()
                    .setTopic(MOVIE_TOPIC)
                    .putData("title", "New Movie!")
                    .putData("path", path)
                    .putData("body", title)
                    .setNotification(Notification.builder()
                            .setBody(title)
                            .setTitle("New Movie!")
                            .build())
                    .build();

            try {
                firebaseApp.send(msg);
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send push notification.", e);
            }
        }
    }
}