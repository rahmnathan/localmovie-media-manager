package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter;
import com.github.rahmnathan.omdb.boundary.MediaProvider;
import com.github.rahmnathan.omdb.boundary.MediaProviderOmdb;
import com.github.rahmnathan.omdb.boundary.MediaProviderStub;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener;
import org.springframework.boot.actuate.metrics.data.RepositoryTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;

import java.util.Set;

@Configuration
@AllArgsConstructor
@EnableAspectJAutoProxy
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
    public MediaProvider createMovieProvider(CamelContext context, ProducerTemplate template){
        if(serviceConfig.getOmdb().isEnabled()) {
            context.setUseMDCLogging(true);
            return new MediaProviderOmdb(context, template, serviceConfig.getOmdb().getApiKey());
        }

        return new MediaProviderStub();
    }

    // Attempt to fix metrics bug https://github.com/spring-projects/spring-boot/issues/26630
    @Bean
    public static MetricsRepositoryMethodInvocationListener metricsRepositoryMethodInvocationListener(
            MetricsProperties metricsProperties, @Lazy MeterRegistry registry, RepositoryTagsProvider tagsProvider) {
        MetricsProperties.Data.Repository properties = metricsProperties.getData().getRepository();
        return new MetricsRepositoryMethodInvocationListener(registry, tagsProvider, properties.getMetricName(),
                properties.getAutotime());
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
