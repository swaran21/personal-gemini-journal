package com.pm.personalgeminijournalbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UserRateLimitFilterTest {
    private final UserRateLimitFilter filter = new UserRateLimitFilter(
            new ObjectMapper(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 2, 1, 3);

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void limitsJournalWritesByVerifiedPrincipalAndReturnsRetryMetadata() throws Exception {
        authenticate("user-a");
        assertEquals(200, invoke("POST", "/api/journal/entry").status());
        assertEquals(200, invoke("POST", "/api/journal/entry").status());
        Result limited = invoke("POST", "/api/journal/entry");
        assertEquals(429, limited.status());
        assertEquals("3600", limited.response().getHeader("Retry-After"));
        assertTrue(limited.response().getContentAsString().contains("Too many requests"));
    }

    @Test void isolatesBucketsBetweenUsersAndPolicies() throws Exception {
        authenticate("user-a");
        assertEquals(200, invoke("POST", "/api/chat/rag").status());
        assertEquals(429, invoke("POST", "/api/chat/rag").status());
        assertEquals(200, invoke("GET", "/api/action-items").status());
        authenticate("user-b");
        assertEquals(200, invoke("POST", "/api/chat/rag").status());
    }

    @Test void weeklyReflectionSharesTheCostSensitiveAiQuota() throws Exception {
        authenticate("user-a");
        assertEquals(200, invoke("POST", "/api/reflections/weekly").status());
        assertEquals(429, invoke("POST", "/api/reflections/weekly").status());
    }

    @Test void doesNotRateLimitBeforeAuthenticationOrOutsideApi() throws Exception {
        assertEquals(200, invoke("POST", "/api/journal/entry").status());
        authenticate("user-a");
        for (int index = 0; index < 5; index++) assertEquals(200, invoke("GET", "/actuator/health").status());
    }

    private void authenticate(String uid) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new FirebasePrincipal(uid), "token", List.of()));
    }

    private Result invoke(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger calls = new AtomicInteger();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> calls.incrementAndGet());
        if (calls.get() == 1 && response.getStatus() == 200) return new Result(200, response);
        return new Result(response.getStatus(), response);
    }

    private record Result(int status, MockHttpServletResponse response) { }
}
