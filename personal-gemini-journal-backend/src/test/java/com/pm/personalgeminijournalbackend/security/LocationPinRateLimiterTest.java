package com.pm.personalgeminijournalbackend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocationPinRateLimiterTest {
    @Test
    void limitsOnlyTheVerifiedOwnersLocationPins() {
        LocationPinRateLimiter limiter = new LocationPinRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 2);
        assertDoesNotThrow(() -> limiter.check("owner-a"));
        assertDoesNotThrow(() -> limiter.check("owner-a"));
        LocationPinRateLimitExceededException limited = assertThrows(LocationPinRateLimitExceededException.class, () -> limiter.check("owner-a"));
        assertEquals(3600, limited.retryAfterSeconds());
        assertDoesNotThrow(() -> limiter.check("owner-b"));
    }
}
