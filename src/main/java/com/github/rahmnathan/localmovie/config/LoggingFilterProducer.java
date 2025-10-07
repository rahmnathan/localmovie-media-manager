package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.localmovie.web.filter.LoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingFilterProducer {

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilter(){
        FilterRegistrationBean<LoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LoggingFilter());
        registrationBean.addUrlPatterns("/localmovies/*");
        return registrationBean;
    }
}
