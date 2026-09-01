package com.pm.personalgeminijournalbackend.reflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReflection(
        LocalDate weekStart,
        LocalDate weekEnd,
        int entryCount,
        List<String> highlights,
        List<String> accomplishments,
        List<String> unresolvedThemes,
        String suggestedFocus,
        Instant generatedAt) {
    public WeeklyReflection {
        highlights = clean(highlights);
        accomplishments = clean(accomplishments);
        unresolvedThemes = clean(unresolvedThemes);
        suggestedFocus = suggestedFocus == null ? "" : suggestedFocus.trim();
        if (suggestedFocus.length() > 2000) throw new IllegalArgumentException("suggestedFocus must not exceed 2000 characters");
    }
    private static List<String> clean(List<String> values) { return values == null ? List.of() : values.stream().map(String::trim).filter(value -> !value.isBlank() && value.length() <= 1000).distinct().limit(10).toList(); }
}
