package com.github.rahmnathan.localmovie.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class AccessTokenFilter implements Filter {
    public static final String ACCESS_TOKEN = "access_token";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            log.info("AccessTokenFilter triggered.");
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpServletRequest);

            Optional<String> accessToken = Optional.ofNullable(httpServletRequest.getParameter(ACCESS_TOKEN));
            accessToken.ifPresent(token -> {
                log.info("Adding access token to request.");
                mutableRequest.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            });

            filterChain.doFilter(mutableRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void destroy() {

    }


}