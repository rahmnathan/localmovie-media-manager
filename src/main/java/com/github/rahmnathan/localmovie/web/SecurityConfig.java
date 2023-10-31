package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.web.filter.AccessTokenFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
class SecurityConfig {

    @Order(1)
    @Bean
    public SecurityFilterChain anonymousAccessFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new AccessTokenFilter(), WebAsyncManagerIntegrationFilter.class)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.requestMatchers("/actuator/**", "/forbidden.css")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .oauth2Login(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}