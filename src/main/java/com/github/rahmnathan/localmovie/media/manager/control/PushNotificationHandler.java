package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.google.pushnotification.boundary.FirebaseNotificationService;
import com.github.rahmnathan.google.pushnotification.data.PushNotification;
import com.github.rahmnathan.localmovie.domain.AndroidPushClient;
import com.github.rahmnathan.localmovie.media.manager.repository.AndroidPushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PushNotificationHandler {
    private final Logger logger = LoggerFactory.getLogger(PushNotificationHandler.class);
    private final FirebaseNotificationService notificationService;
    private final AndroidPushTokenRepository pushTokenRepository;

    public PushNotificationHandler(AndroidPushTokenRepository pushTokenRepository, FirebaseNotificationService notificationService) {
        this.notificationService = notificationService;
        this.pushTokenRepository = pushTokenRepository;
    }

    public void addPushToken(AndroidPushClient pushClient) {
        Optional<AndroidPushClient> optionalPushClient = pushTokenRepository.findById(pushClient.getDeviceId());
        if (optionalPushClient.isPresent()) {
            AndroidPushClient managedPushClient = optionalPushClient.get();
            if (!managedPushClient.getPushToken().equals(pushClient.getPushToken())) {
                managedPushClient.setPushToken(pushClient.getPushToken());
            }
        } else {
            pushTokenRepository.save(pushClient);
        }
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
