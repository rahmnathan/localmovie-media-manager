package com.github.rahmnathan.localmovie.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter implements Filter {
    public static final String X_CORRELATION_ID = "x-correlation-id";
    private static final String CLIENT_ADDRESS = "client-address";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

            String correlationId = httpServletRequest.getHeader(X_CORRELATION_ID);
            MDC.put(X_CORRELATION_ID, StringUtils.isEmpty(correlationId) ? UUID.randomUUID().toString() : correlationId);

            String clientAddress = ((HttpServletRequest) servletRequest).getHeader("X-FORWARDED-FOR");
            MDC.put(CLIENT_ADDRESS, clientAddress);

            filterChain.doFilter(httpServletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }
}