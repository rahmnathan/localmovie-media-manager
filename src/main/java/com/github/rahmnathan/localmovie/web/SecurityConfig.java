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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

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
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Start with default scope-based authorities
            JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> authorities = new HashSet<>(defaultConverter.convert(jwt));

            // Add Keycloak realm roles
            authorities.addAll(extractKeycloakRoles(jwt.getClaims()));

            return authorities;
        });

        http.authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.requestMatchers(
                                        "/actuator/**",
                                        "/forbidden.css",
                                        "/localmovie/v1/signed/media/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html")
                                .permitAll()
//                                .requestMatchers("/admin/**")
//                                .hasRole("movie-admin")
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
                    oauth2.bearerTokenResolver(resolver);
                })
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())))
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }

    /**
     * Maps Keycloak roles from the OIDC token to Spring Security GrantedAuthorities.
     * Keycloak stores roles in realm_access.roles claim.
     */
    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(authorities);

            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    Map<String, Object> claims = oidcUserAuthority.getIdToken().getClaims();
                    mappedAuthorities.addAll(extractKeycloakRoles(claims));
                } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    Map<String, Object> attributes = oauth2UserAuthority.getAttributes();
                    mappedAuthorities.addAll(extractKeycloakRoles(attributes));
                }
            }

            return mappedAuthorities;
        };
    }

    @SuppressWarnings("unchecked")
    private Collection<SimpleGrantedAuthority> extractKeycloakRoles(Map<String, Object> claims) {
        Set<SimpleGrantedAuthority> roles = new HashSet<>();

        // Extract realm roles from realm_access.roles
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object rolesObj = realmAccessMap.get("roles");
            if (rolesObj instanceof List<?> rolesList) {
                for (Object role : rolesList) {
                    if (role instanceof String roleStr) {
                        roles.add(new SimpleGrantedAuthority("ROLE_" + roleStr));
                    }
                }
            }
        }

        // Also check for roles directly in claims (some Keycloak configs)
        Object directRoles = claims.get("roles");
        if (directRoles instanceof List<?> rolesList) {
            for (Object role : rolesList) {
                if (role instanceof String roleStr) {
                    roles.add(new SimpleGrantedAuthority("ROLE_" + roleStr));
                }
            }
        }

        return roles;
    }
}
