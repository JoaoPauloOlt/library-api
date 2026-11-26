package com.jpoltramari.library_api.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PublicEndpointRateLimitFilterTest {

    private PublicEndpointRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        PublicRateLimitService service = new PublicRateLimitService(
                2, Duration.ofMinutes(1),
                2, Duration.ofHours(1),
                3, Duration.ofMinutes(15),
                "test-hmac-secret");
        filter = new PublicEndpointRateLimitFilter(service);
    }

    @Test
    void shouldRejectLoginIpAfterLimitAndSetRetryAfter() throws Exception {
        MockHttpServletResponse first = invoke("POST", "/auth/login", "192.0.2.1");
        MockHttpServletResponse second = invoke("POST", "/auth/login", "192.0.2.1");
        MockHttpServletResponse third = invoke("POST", "/auth/login", "192.0.2.1");

        assertEquals(200, first.getStatus());
        assertEquals(200, second.getStatus());
        assertEquals(429, third.getStatus());
        assertEquals("60", third.getHeader("Retry-After"));
        assertTrue(third.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void shouldApplyRegistrationLimitOnlyToRegistrationEndpoint() throws Exception {
        assertEquals(200, invoke("POST", "/users", "192.0.2.2").getStatus());
        assertEquals(200, invoke("POST", "/users", "192.0.2.2").getStatus());
        assertEquals(429, invoke("POST", "/users", "192.0.2.2").getStatus());
        assertEquals(200, invoke("GET", "/users", "192.0.2.2").getStatus());
    }

    private MockHttpServletResponse invoke(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
