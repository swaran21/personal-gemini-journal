package com.pm.personalgeminijournalbackend.chat;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemporalQueryResolver {
    private static final Pattern LAST_HOURS = Pattern.compile("\\b(?:last|past)\\s+(\\d{1,3})\\s+hours?\\b", Pattern.CASE_INSENSITIVE);
    private final Clock clock;

    public TemporalQueryResolver() { this(Clock.systemUTC()); }
    TemporalQueryResolver(Clock clock) { this.clock = clock; }

    public Optional<TimeRange> resolve(String question, ZoneId zone) {
        Instant now = clock.instant();
        Matcher hours = LAST_HOURS.matcher(question);
        if (hours.find()) {
            int value = Integer.parseInt(hours.group(1));
            if (value < 1 || value > 720) throw new IllegalArgumentException("relative hour range must be between 1 and 720");
            return Optional.of(new TimeRange(now.minus(Duration.ofHours(value)), now, "the last " + value + " hours"));
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (normalized.contains("yesterday")) return Optional.of(days(today.minusDays(1), today, zone, "yesterday"));
        if (normalized.contains("today")) return Optional.of(new TimeRange(today.atStartOfDay(zone).toInstant(), now, "today"));
        if (normalized.contains("last week")) {
            LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return Optional.of(days(thisMonday.minusWeeks(1), thisMonday, zone, "last week"));
        }
        if (normalized.contains("this week") || normalized.contains("current week")) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return Optional.of(new TimeRange(monday.atStartOfDay(zone).toInstant(), now, "this week"));
        }
        return Optional.empty();
    }

    public Instant now() { return clock.instant(); }

    private TimeRange days(LocalDate start, LocalDate end, ZoneId zone, String label) {
        return new TimeRange(start.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant(), label);
    }

    public record TimeRange(Instant startInclusive, Instant endExclusive, String label) { }
}
