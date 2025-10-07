package com.github.rahmnathan.localmovie.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.util.UUID;


public class LoggingFilter implements Filter {
    public static final String X_CORRELATION_ID = "x-correlation-id";
    private static final String CLIENT_ADDRESS = "client-address";
    private static final String USER_AGENT = "user-agent";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

            String correlationId = httpServletRequest.getHeader(X_CORRELATION_ID);
            MDC.put(X_CORRELATION_ID, StringUtils.isEmpty(correlationId) ? UUID.randomUUID().toString() : correlationId);

            String clientAddress = httpServletRequest.getHeader("X-Forwarded-For");
            MDC.put(CLIENT_ADDRESS, clientAddress);

            String userAgent = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
            MDC.put(USER_AGENT, userAgent);

            filterChain.doFilter(httpServletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }
}