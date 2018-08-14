package com.github.rahmnathan.localmovie.media.manager.boundary;

import com.github.rahmnathan.localmovie.domain.AndroidPushClient;
import com.github.rahmnathan.localmovie.media.manager.control.PushNotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class MediaManagerWeb {
    private final Logger logger = LoggerFactory.getLogger(MediaManagerWeb.class);
    private static final String TRANSACTION_ID = "TransactionId";
    private final PushNotificationHandler notificationHandler;

    public MediaManagerWeb(PushNotificationHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    @PostMapping(path = "/api/v1/client/Android", consumes = APPLICATION_JSON_VALUE)
    public void putAndroidClient(@RequestBody AndroidPushClient pushClient){
        logger.info("Request received to store Android push client: {}", pushClient);
        MDC.put(TRANSACTION_ID, UUID.randomUUID().toString());

        notificationHandler.addPushToken(pushClient);
    }
}
