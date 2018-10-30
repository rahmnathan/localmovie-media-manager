package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.google.pushnotification.boundary.FirebaseNotificationService;
import com.github.rahmnathan.google.pushnotification.data.PushNotification;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationHandler {
    private final Logger logger = LoggerFactory.getLogger(PushNotificationHandler.class);
    private static final String MOVIE_TOPIC = "movies";
    private final FirebaseNotificationService notificationService;

    public PushNotificationHandler(ProducerTemplate template, CamelContext context) {
        this.notificationService = new FirebaseNotificationService(template, context);
    }

    void sendPushNotifications(String fileName, String path) {
        logger.info("Sending notification of new movie: {} to {} clients", path, MOVIE_TOPIC);
        PushNotification pushNotification = PushNotification.Builder.newInstance()
                .setTopic(MOVIE_TOPIC)
                .setTitle("New Movie!")
                .setBody(fileName)
                .addData("path", path)
                .build();

        notificationService.sendPushNotification(pushNotification);
    }
}