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

    public WeeklyReflectionService(JournalRepository repository, GenerativeAiService ai) { this.repository = repository; this.ai = ai; }

    public WeeklyReflection generate(String uid, WeeklyReflectionRequest request) {
        ZoneId zone = zone(request.timeZone());
        LocalDate requested = request.weekStart() == null ? LocalDate.now(zone) : request.weekStart();
        LocalDate start = requested.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(7);
        if (start.isAfter(LocalDate.now(zone))) throw new IllegalArgumentException("weekStart cannot be in the future");
        List<JournalEntry> entries = repository.entriesBetween(uid, start.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant(), 100);
        if (entries.isEmpty()) return new WeeklyReflection(start, end.minusDays(1), 0, List.of(), List.of(), List.of(), "", Instant.now());
        WeeklyReflection generated = ai.generateWeeklyReflection(entries);
        return new WeeklyReflection(start, end.minusDays(1), entries.size(), generated.highlights(), generated.accomplishments(), generated.unresolvedThemes(), generated.suggestedFocus(), Instant.now());
    }

    private ZoneId zone(String value) {
        try { return value == null || value.isBlank() ? ZoneOffset.UTC : ZoneId.of(value); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("Invalid timeZone"); }
    }
}
