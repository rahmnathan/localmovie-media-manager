package com.github.rahmnathan.localmovie.web;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
class SecurityConfig {

    @Order(1)
    @Bean
    public SecurityFilterChain anonymousAccessFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizeRequests -> {
            authorizeRequests.requestMatchers("/actuator/**", "/forbidden.css")
                    .permitAll()
                    .anyRequest()
                    .authenticated();
        });

        http.oauth2ResourceServer(Customizer.withDefaults());
        http.oauth2Login(Customizer.withDefaults());
        http.csrf().disable();

        return http.build();
    }
}