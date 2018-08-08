package com.github.rahmnathan.localmovie.media.manager.config;

import com.github.rahmnathan.omdb.boundary.OmdbMovieProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetadataProviderProducer {

    @Bean
    public OmdbMovieProvider buildMovieProvider(CamelContext context, ProducerTemplate template, @Value("${omdb.api.key}") String apiKey){
        return new OmdbMovieProvider(context, template, apiKey);
    }
}
