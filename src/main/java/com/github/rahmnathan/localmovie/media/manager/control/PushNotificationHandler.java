package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.google.pushnotification.boundary.FirebaseNotificationService;
import com.github.rahmnathan.google.pushnotification.data.PushNotification;
import com.github.rahmnathan.localmovie.media.manager.repository.AndroidPushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationHandler {
    private final Logger logger = LoggerFactory.getLogger(PushNotificationHandler.class);
    private final FirebaseNotificationService notificationService;
    private final AndroidPushTokenRepository pushTokenRepository;

    public PushNotificationHandler(AndroidPushTokenRepository pushTokenRepository, FirebaseNotificationService notificationService) {
        this.notificationService = notificationService;
        this.pushTokenRepository = pushTokenRepository;
    }

    void sendPushNotifications(String fileName, String path) {
        logger.info("Sending notification of new movie: {} to {} clients", path, pushTokenRepository.count());
        pushTokenRepository.findAll().forEach(token -> {
            PushNotification pushNotification = PushNotification.Builder.newInstance()
                    .setRecipientToken(token.getPushToken())
                    .setTitle("New Movie!")
                    .setBody(fileName)
                    .addData("path", path)
                    .build();

            notificationService.sendPushNotification(pushNotification);
        });
    }
}
