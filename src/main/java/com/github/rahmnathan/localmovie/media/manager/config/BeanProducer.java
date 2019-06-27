package com.github.rahmnathan.localmovie.media.manager.config;

import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class BeanProducer {
    private final MediaManagerConfig mediaManagerConfig;
    private final ProducerTemplate template;
    private final CamelContext context;

    public BeanProducer(CamelContext context, ProducerTemplate template, MediaManagerConfig mediaManagerConfig) {
        this.mediaManagerConfig = mediaManagerConfig;
        this.template = template;
        this.context = context;
    }

    @Bean
    public JedisPool createJedisPool(){
        return new JedisPool(mediaManagerConfig.getJedisHost());
    }

    @Bean
    public OmdbMediaProvider createMovieProvider(){
        context.setUseMDCLogging(true);
        return new OmdbMediaProvider(context, template, mediaManagerConfig.getOmdbApiKey());
    }
}
