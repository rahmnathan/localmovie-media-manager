package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.google.pushnotification.boundary.FirebaseNotificationService;
import com.github.rahmnathan.google.pushnotification.data.PushNotification;
import com.github.rahmnathan.localmovie.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushNotificationService {
    private static final String MOVIE_TOPIC = "movies";
    private final FirebaseNotificationService notificationService;
    private final boolean enabled;

    public PushNotificationService(ProducerTemplate template, CamelContext context, ServiceConfig serviceConfig) {
        this.notificationService = new FirebaseNotificationService(template, context);
        this.enabled = serviceConfig.isNotificationsEnabled();
    }

    void sendPushNotifications(String fileName, String path) {
        if (enabled) {
            log.info("Sending notification of new movie: {} to {} clients", path, MOVIE_TOPIC);
            PushNotification pushNotification = PushNotification.Builder.newInstance()
                    .setTopic(MOVIE_TOPIC)
                    .setTitle("New Movie!")
                    .setBody(fileName)
                    .addData("path", path)
                    .build();

            notificationService.sendPushNotification(pushNotification);
        }
    }
}