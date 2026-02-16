package com.github.rahmnathan.localmovie.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityUtils {
    private static final String DEFAULT_USER = "movieuser";

    public String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String name = authentication.getName();
            log.debug("Authentication principal: {}, name: {}", authentication.getPrincipal(), name);
            return name;
        }
        return DEFAULT_USER;
    }
}
