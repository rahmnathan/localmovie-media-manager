package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.omdb.boundary.MediaProviderOmdb;
import com.github.rahmnathan.omdb.boundary.MediaProviderStub;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaProviderProducer {

    @Bean
    public com.github.rahmnathan.omdb.boundary.MediaProvider createMovieProvider(CamelContext context, ProducerTemplate template, ServiceConfig serviceConfig){
        if(serviceConfig.getOmdb().isEnabled()) {
            context.setUseMDCLogging(true);
            return new MediaProviderOmdb(context, template, serviceConfig.getOmdb().getApiKey());
        }

        return new MediaProviderStub();
    }
}
