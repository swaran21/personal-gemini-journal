package com.pm.personalgeminijournalbackend.security;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits optional location-bearing journal writes by the verified user identity.
 * This protects the consent surface today and provides a conservative boundary before
 * any future paid location enrichment is considered. It never performs Maps API calls.
 */
public class LocationPinRateLimiter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int limit;

    public LocationPinRateLimiter(int limit) { this(Clock.systemUTC(), limit); }
    LocationPinRateLimiter(Clock clock, int limit) { this.clock = clock; this.limit = Math.max(1, Math.min(limit, 1_000)); }

    public void check(String uid) {
        long now = clock.millis();
        Window window = windows.compute(uid, (ignored, current) -> current == null || current.expiresAt <= now
                ? new Window(1, now + 3_600_000L) : new Window(current.count + 1, current.expiresAt));
        if (window.count > limit) throw new LocationPinRateLimitExceededException(Math.max(1, (window.expiresAt - now + 999) / 1_000));
    }

    private record Window(int count, long expiresAt) { }
}
