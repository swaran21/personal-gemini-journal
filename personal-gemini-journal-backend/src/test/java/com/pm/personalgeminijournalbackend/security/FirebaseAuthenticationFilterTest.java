package com.pm.personalgeminijournalbackend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseAuthenticationFilterTest {
    private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    private final FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(firebaseAuth);

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void rejectsMissingMalformedAndBlankBearerTokens() throws Exception {
        for (String header : new String[] {null, "Basic abc", "Bearer    "}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/action-items");
            if (header != null) request.addHeader("Authorization", header);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain);
        }
        verifyNoInteractions(firebaseAuth);
    }

    @Test void rejectsInvalidFirebaseToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/action-items");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(firebaseAuth.verifyIdToken("bad-token", true)).thenThrow(new IllegalArgumentException("invalid"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test void authenticatesVerifiedTokenAndForwardsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/action-items");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-1");
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(token);

        filter.doFilter(request, response, chain);

        assertEquals("uid-1", ((FirebasePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).uid());
        verify(chain).doFilter(request, response);
    }

    @Test void skipsHealthAndCorsPreflightRequests() {
        MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/action-items");
        preflight.addHeader("Origin", "http://localhost:3000");
        preflight.addHeader("Access-Control-Request-Method", "GET");

        assertTrue(filter.shouldNotFilter(health));
        assertTrue(filter.shouldNotFilter(preflight));
    }
}
