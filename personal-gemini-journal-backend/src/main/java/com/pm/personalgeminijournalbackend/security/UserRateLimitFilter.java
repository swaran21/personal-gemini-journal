package com.pm.personalgeminijournalbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-instance authenticated-user quota guard. It never trusts IP addresses or a UID supplied by the client.
 * A shared gateway or distributed store must replace this component when Cloud Run scales beyond one instance.
 */
public class UserRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_BUCKETS = 10_000;
    private final Map<BucketKey, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Policy journalWrites;
    private final Policy ragQueries;
    private final Policy ordinaryApi;

    public UserRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.journal-writes-per-hour:30}") int journalWritesPerHour,
            @Value("${app.rate-limit.rag-queries-per-hour:20}") int ragQueriesPerHour,
            @Value("${app.rate-limit.api-requests-per-minute:120}") int apiRequestsPerMinute) {
        this(objectMapper, Clock.systemUTC(), journalWritesPerHour, ragQueriesPerHour, apiRequestsPerMinute);
    }

    UserRateLimitFilter(ObjectMapper objectMapper, Clock clock, int journalLimit, int ragLimit, int apiLimit) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.journalWrites = new Policy("journal-write", bounded(journalLimit), 3_600_000L);
        this.ragQueries = new Policy("rag-query", bounded(ragLimit), 3_600_000L);
        this.ordinaryApi = new Policy("api", bounded(apiLimit), 60_000L);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Policy policy = policy(request);
        FirebasePrincipal principal = principal();
        if (policy == null || principal == null) {
            chain.doFilter(request, response);
            return;
        }

        long now = clock.millis();
        Window window = windows.compute(new BucketKey(principal.uid(), policy.name()), (key, current) ->
                current == null || current.expiresAt <= now
                        ? new Window(1, now + policy.windowMillis())
                        : new Window(current.count + 1, current.expiresAt));
        cleanupPeriodically(now);

        long retryAfterSeconds = Math.max(1, (window.expiresAt - now + 999) / 1000);
        response.setHeader("X-RateLimit-Limit", Integer.toString(policy.limit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(Math.max(0, policy.limit() - window.count)));
        response.setHeader("X-RateLimit-Reset", Long.toString(window.expiresAt / 1000));
        if (window.count <= policy.limit()) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "type", "about:blank", "title", "Too many requests", "status", 429,
                "detail", "Your request limit was reached. Please retry later."));
    }

    private Policy policy(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod()) && "/api/journal/entry".equals(path)) return journalWrites;
        if ("POST".equals(request.getMethod()) && "/api/chat/rag".equals(path)) return ragQueries;
        return path.startsWith("/api/") ? ordinaryApi : null;
    }

    private FirebasePrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof FirebasePrincipal principal ? principal : null;
    }

    private void cleanupPeriodically(long now) {
        if (requests.incrementAndGet() % 1_000 != 0 && windows.size() <= MAX_TRACKED_BUCKETS) return;
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (windows.size() > MAX_TRACKED_BUCKETS) {
            windows.keySet().stream().limit(windows.size() - MAX_TRACKED_BUCKETS).toList().forEach(windows::remove);
        }
    }

    private static int bounded(int configured) { return Math.max(1, Math.min(configured, 100_000)); }
    private record Policy(String name, int limit, long windowMillis) { }
    private record BucketKey(String uid, String policy) { }
    private record Window(int count, long expiresAt) { }
}
