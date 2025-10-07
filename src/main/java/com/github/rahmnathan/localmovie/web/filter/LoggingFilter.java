package com.github.rahmnathan.localmovie.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.UUID;


@Slf4j
public class LoggingFilter implements Filter {
    public static final String X_CORRELATION_ID = "x-correlation-id";
    private static final String CLIENT_ADDRESS = "client-address";
    private static final String USER_AGENT = "user-agent";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

            httpServletRequest.getHeaderNames().asIterator().forEachRemaining(header -> log.debug("{}: {}", header, httpServletRequest.getHeader(header)));

            MDC.put(X_CORRELATION_ID, getxCorrelationId(httpServletRequest));

            String clientAddress = httpServletRequest.getHeader("x-original-forwarded-for");
            MDC.put(CLIENT_ADDRESS, clientAddress);

            String userAgent = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
            MDC.put(USER_AGENT, userAgent);

            filterChain.doFilter(httpServletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }

    private String getxCorrelationId(HttpServletRequest servletRequest) {
        String correlationId = servletRequest.getHeader(X_CORRELATION_ID);

        if (StringUtils.isBlank(correlationId)) {
            correlationId = servletRequest.getHeader("x-request-id");;
        }

        if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        return correlationId;
    }
}