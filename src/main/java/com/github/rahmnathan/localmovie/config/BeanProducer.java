package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter;
import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanProducer {
    private final ServiceConfig serviceConfig;

    public BeanProducer(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> loggingFilter(){
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.addUrlPatterns("/localmovies/*");
        return registrationBean;
    }

    @Bean
    public OmdbMediaProvider createMovieProvider(CamelContext context, ProducerTemplate template){
        context.setUseMDCLogging(true);
        return new OmdbMediaProvider(context, template, serviceConfig.getOmdbApiKey());
    }
}
