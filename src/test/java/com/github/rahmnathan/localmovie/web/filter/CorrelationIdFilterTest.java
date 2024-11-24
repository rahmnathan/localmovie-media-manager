package com.github.rahmnathan.localmovie.web.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();

    @Test
    void doFilterTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String requestId = UUID.randomUUID().toString();
        request.addHeader(X_CORRELATION_ID, requestId);
        correlationIdFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(MDC.get(X_CORRELATION_ID));
    }
}
