package com.github.rahmnathan.localmovie.media.manager.config;

import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaProviderProducer {
    private final CamelContext context;
    private final ProducerTemplate template;
    private final String apiKey;

    public MediaProviderProducer(CamelContext context, ProducerTemplate template, MediaManagerConfig mediaManagerConfig) {
        this.apiKey = mediaManagerConfig.getOmdbApiKey();
        this.template = template;
        this.context = context;
    }

    @Bean
    public OmdbMediaProvider createMovieProvider(){
        context.setUseMDCLogging(true);
        return new OmdbMediaProvider(context, template, apiKey);
    }
}
