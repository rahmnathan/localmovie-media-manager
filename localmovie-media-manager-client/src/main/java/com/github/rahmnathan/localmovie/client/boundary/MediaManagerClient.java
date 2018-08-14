package com.github.rahmnathan.localmovie.client.boundary;

import com.github.rahmnathan.localmovie.client.config.MediaClientConfig;
import com.github.rahmnathan.localmovie.domain.AndroidPushClient;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.rahmnathan.localmovie.client.boundary.MediaManagerRoutes.ANDROID_PUSH_CLIENT_ROUTE;

public class MediaManagerClient {
    private final Logger logger = LoggerFactory.getLogger(MediaManagerClient.class);
    private final ProducerTemplate template;

    public MediaManagerClient(ProducerTemplate template, CamelContext context, MediaClientConfig clientConfig) {
        new MediaManagerRoutes(context, clientConfig).initialize();
        this.template = template;
    }

    public void addPushToken(AndroidPushClient pushClient){
        logger.info("Request received to upsert pushClient: {}", pushClient);
        template.asyncRequestBody(ANDROID_PUSH_CLIENT_ROUTE, pushClient);
    }
}
