package com.pm.personalgeminijournalbackend.reflection;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class WeeklyReflectionService {
    private final JournalRepository repository;
    private final GenerativeAiService ai;
    private final Clock clock;

    public WeeklyReflectionService(JournalRepository repository, GenerativeAiService ai) { this(repository, ai, Clock.systemUTC()); }
    WeeklyReflectionService(JournalRepository repository, GenerativeAiService ai, Clock clock) { this.repository = repository; this.ai = ai; this.clock = clock; }

    public WeeklyReflection generate(String uid, WeeklyReflectionRequest request) {
        ZoneId zone = zone(request.timeZone());
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate requested = request.weekStart() == null ? today : request.weekStart();
        LocalDate start = requested.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(7);
        if (start.isAfter(today)) throw new IllegalArgumentException("weekStart cannot be in the future");
        Instant rangeEnd = end.atStartOfDay(zone).toInstant();
        if (!rangeEnd.isBefore(now)) rangeEnd = now;
        List<JournalEntry> entries = repository.entriesBetween(uid, start.atStartOfDay(zone).toInstant(), rangeEnd, 100);
        if (entries.isEmpty()) return new WeeklyReflection(start, end.minusDays(1), 0, List.of(), List.of(), List.of(), "", now);
        WeeklyReflection generated = ai.generateWeeklyReflection(entries);
        List<String> highlights = generated.highlights().isEmpty()
                ? List.of("You recorded: \"" + excerpt(entries.get(entries.size() - 1).text()) + "\"")
                : generated.highlights();
        String focus = generated.suggestedFocus().isBlank()
                ? "Choose one small next step connected to: \"" + excerpt(entries.get(entries.size() - 1).text()) + "\""
                : generated.suggestedFocus();
        return new WeeklyReflection(start, end.minusDays(1), entries.size(), highlights, generated.accomplishments(), generated.unresolvedThemes(), focus, now);
    }

    private ZoneId zone(String value) {
        try { return value == null || value.isBlank() ? ZoneOffset.UTC : ZoneId.of(value); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("Invalid timeZone"); }
    }

    private String excerpt(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 177) + "...";
    }
}
