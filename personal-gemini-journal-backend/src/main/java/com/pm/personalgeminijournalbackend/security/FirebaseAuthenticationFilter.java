package com.pm.personalgeminijournalbackend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final FirebaseAuth firebaseAuth;
    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) { this.firebaseAuth = firebaseAuth; }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health") || CorsUtils.isPreFlightRequest(request);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) { unauthorized(response); return; }
        try {
            String token = header.substring(7).trim();
            if (token.isEmpty()) throw new IllegalArgumentException("blank token");
            FirebaseToken verified = firebaseAuth.verifyIdToken(token, true);
            var auth = new UsernamePasswordAuthenticationToken(new FirebasePrincipal(verified.getUid()), null, List.of());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (Exception ignored) { SecurityContextHolder.clearContext(); unauthorized(response); }
    }
    private void unauthorized(HttpServletResponse response) throws IOException { response.setStatus(401); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE); response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401}"); }
}
