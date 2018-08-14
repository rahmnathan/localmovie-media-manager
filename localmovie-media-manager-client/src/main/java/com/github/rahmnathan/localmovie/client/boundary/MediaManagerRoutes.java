package com.github.rahmnathan.localmovie.client.boundary;

import com.github.rahmnathan.localmovie.client.config.MediaClientConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MediaManagerRoutes {
    private final Logger logger = LoggerFactory.getLogger(MediaManagerRoutes.class);
    private static final String ANDROID_PUSH_ENDPOINT = "/api/v1/client/Android";
    static final String ANDROID_PUSH_CLIENT_ROUTE = "androidPushRoute";
    private final MediaClientConfig clientConfig;
    private final CamelContext context;

    MediaManagerRoutes(CamelContext context, MediaClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.context = context;
    }

    void initialize(){
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from(ANDROID_PUSH_CLIENT_ROUTE)
                            .marshal().json(JsonLibrary.Jackson)
                            .setHeader(Exchange.HTTP_URI, constant(ANDROID_PUSH_ENDPOINT))
                            .to(processUrl(clientConfig.getMediaManagerUrl()))
                            .end();
                }
            });
        } catch (Exception e){
            logger.error("Failure adding Camel routes to context.");
        }
    }

    private String processUrl(String url){
        if(url != null){
            if(url.startsWith("https")){
                return url.replace("https", "https4");
            }
            if(url.startsWith("http")){
                return url.replace("http", "http4");
            }
        }

        logger.error("Invalid media manager url.");
        return url;
    }
}
