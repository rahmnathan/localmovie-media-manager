package com.github.rahmnathan.localmovie.media.manager.config;

import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaProviderProducer {
    private final CamelContext context;
    private final ProducerTemplate template;
    private final String apiKey;

    public MediaProviderProducer(CamelContext context, ProducerTemplate template, @Value("${omdb.api.key}") String apiKey) {
        this.context = context;
        this.template = template;
        this.apiKey = apiKey;
    }

    @Bean
    public OmdbMediaProvider createMovieProvider(){
        context.setUseMDCLogging(true);
        return new OmdbMediaProvider(context, template, apiKey);
    }
}
