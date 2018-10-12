package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.google.pushnotification.boundary.FirebaseNotificationService;
import com.github.rahmnathan.google.pushnotification.data.PushNotification;
import com.github.rahmnathan.localmovie.domain.AndroidPushClient;
import com.github.rahmnathan.localmovie.media.manager.repository.AndroidPushTokenRepository;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PushNotificationHandler {
    private final Logger logger = LoggerFactory.getLogger(PushNotificationHandler.class);
    private final FirebaseNotificationService notificationService;
    private final AndroidPushTokenRepository pushTokenRepository;

    public PushNotificationHandler(AndroidPushTokenRepository pushTokenRepository, ProducerTemplate template, CamelContext context) {
        this.pushTokenRepository = pushTokenRepository;
        this.notificationService = new FirebaseNotificationService(template, context);
    }

    @Transactional(readOnly = true)
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

    @Transactional
    public void addPushToken(AndroidPushClient pushClient) {
        Optional<AndroidPushClient> optionalPushClient = pushTokenRepository.findById(pushClient.getDeviceId());
        if (optionalPushClient.isPresent()) {
            AndroidPushClient managedPushClient = optionalPushClient.get();
            managedPushClient.setLastAccess(LocalDate.now());
            if (!managedPushClient.getPushToken().equals(pushClient.getPushToken())) {
                managedPushClient.setPushToken(pushClient.getPushToken());
            }
        } else {
            pushClient.setLastAccess(LocalDate.now());
            pushTokenRepository.save(pushClient);
        }
    }
}
