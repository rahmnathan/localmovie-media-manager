package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter;
import com.github.rahmnathan.omdb.boundary.MediaProvider;
import com.github.rahmnathan.omdb.boundary.MediaProviderOmdb;
import com.github.rahmnathan.omdb.boundary.MediaProviderStub;
import lombok.AllArgsConstructor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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

    @Bean
    public LockProvider jdbcLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
