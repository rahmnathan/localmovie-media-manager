package com.github.rahmnathan.localmovie.web;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
class SecurityConfig {

    /**
     * Security chain for Cast receiver - needs frame options disabled since
     * Chromecast loads the receiver in an embedded context.
     */
    @Order(1)
    @Bean
    public SecurityFilterChain castReceiverFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/cast/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Order(2)
    @Bean
    public SecurityFilterChain anonymousAccessFilterChain(HttpSecurity http) throws Exception {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        resolver.setAllowUriQueryParameter(true);
        resolver.setAllowFormEncodedBodyParameter(true);

        // Configure JWT to use preferred_username as the principal name
        // This ensures consistency between OAuth2 login (webapp) and JWT bearer tokens (Android)
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setPrincipalClaimName("preferred_username");

        http.authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.requestMatchers("/actuator/**", "/forbidden.css", "/localmovie/v1/signed/media/**")
                                .permitAll()
//                                .requestMatchers("/admin/**")
//                                .hasRole("movie-admin")
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
                    oauth2.bearerTokenResolver(resolver);
                })
                .oauth2Login(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
