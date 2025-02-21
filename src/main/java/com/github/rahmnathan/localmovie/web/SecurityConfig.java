package com.github.rahmnathan.localmovie.web;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
class SecurityConfig {

    @Order(1)
    @Bean
    public SecurityFilterChain anonymousAccessFilterChain(HttpSecurity http) throws Exception {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        resolver.setAllowUriQueryParameter(true);
        resolver.setAllowFormEncodedBodyParameter(true);

        http.authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.requestMatchers("/actuator/**", "/forbidden.css")
                                .permitAll()
//                                .requestMatchers("/admin/**")
//                                .hasRole("movie-admin")
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(Customizer.withDefaults());
                    oauth2.bearerTokenResolver(resolver);
                })
                .oauth2Login(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
