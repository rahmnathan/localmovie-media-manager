package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnProperty(prefix = "service.ollama", name = "enabled", havingValue = "true")
public class OllamaApiConfig {

    @Bean
    public OllamaApi ollamaApi(ServiceConfig serviceConfig) {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();
        engine.setFollowRedirects(true);

        Client client = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .httpEngine(engine)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // Ollama can be slow for large prompts
                .build();

        String baseUrl = serviceConfig.getOllama().getBaseUrl();
        ResteasyWebTarget target = (ResteasyWebTarget) client.target(baseUrl);
        return target.proxy(OllamaApi.class);
    }
}
