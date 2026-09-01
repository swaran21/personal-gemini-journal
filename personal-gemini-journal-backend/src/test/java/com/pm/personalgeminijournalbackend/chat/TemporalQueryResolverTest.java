package com.pm.personalgeminijournalbackend.chat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class TemporalQueryResolverTest {
    private final TemporalQueryResolver resolver = new TemporalQueryResolver(
            Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC));

    @Test void resolvesYesterdayInTheUsersTimeZone() {
        var range = resolver.resolve("What did I do yesterday?", ZoneId.of("Asia/Kolkata")).orElseThrow();
        assertEquals(Instant.parse("2026-08-30T18:30:00Z"), range.startInclusive());
        assertEquals(Instant.parse("2026-08-31T18:30:00Z"), range.endExclusive());
    }

    @Test void resolvesRelativeHoursFromTheCurrentInstant() {
        var range = resolver.resolve("What happened in the last 4 hours?", ZoneId.of("UTC")).orElseThrow();
        assertEquals(Instant.parse("2026-09-01T08:00:00Z"), range.startInclusive());
        assertEquals(Instant.parse("2026-09-01T12:00:00Z"), range.endExclusive());
    }

    @Test void resolvesPreviousHours() {
        var range = resolver.resolve("What did I do in my previous 5 hours?", ZoneId.of("UTC")).orElseThrow();
        assertEquals(Instant.parse("2026-09-01T07:00:00Z"), range.startInclusive());
        assertEquals(Instant.parse("2026-09-01T12:00:00Z"), range.endExclusive());
    }

    @Test void rejectsUnboundedRelativeHourWindows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("past 999 hours", ZoneId.of("UTC")));
    }

    @Test void leavesNonTemporalQuestionsForSemanticRetrieval() {
        assertTrue(resolver.resolve("How did Java learning help me?", ZoneId.of("UTC")).isEmpty());
    }
}
