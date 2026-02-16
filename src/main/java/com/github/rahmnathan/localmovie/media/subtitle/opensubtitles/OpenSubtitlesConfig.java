package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSubtitlesConfig {
    private static final String OPENSUBTITLES_URL = "https://api.opensubtitles.com";
    private static final String USER_AGENT = "localmovies v1.0";

    @Bean
    public Client jaxrsClient() {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();
        engine.setFollowRedirects(true);

        return ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .httpEngine(engine)
                .register(new UserAgentFilter())
                .build();
    }

    @Bean
    public OpenSubtitlesApi openSubtitlesApi(Client jaxrsClient) {
        ResteasyWebTarget target = (ResteasyWebTarget) jaxrsClient.target(OPENSUBTITLES_URL);
        return target.proxy(OpenSubtitlesApi.class);
    }

    private static class UserAgentFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle("User-Agent", USER_AGENT);
        }
    }
}
