package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter;
import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import lombok.AllArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@AllArgsConstructor
public class BeanProducer {
    private final ServiceConfig serviceConfig;

    @Bean
    @ConditionalOnProperty(name = "service.directoryMonitor.enabled", havingValue = "true")
    public DirectoryMonitor buildDirectoryMonitor(Set<DirectoryMonitorObserver> observers) {
        return new DirectoryMonitor(serviceConfig.getMediaPaths(), observers);
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
