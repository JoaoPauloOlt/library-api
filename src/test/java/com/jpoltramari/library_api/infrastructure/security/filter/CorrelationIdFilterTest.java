package com.jpoltramari.library_api.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldReuseSafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        request.addHeader(CorrelationIdFilter.HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("request-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isNotNull()
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void shouldReplaceUnsafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        request.addHeader(CorrelationIdFilter.HEADER, "bad\ncorrelation-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isNotEqualTo("bad\ncorrelation-id")
                .matches("[0-9a-f-]{36}");
    }
}
