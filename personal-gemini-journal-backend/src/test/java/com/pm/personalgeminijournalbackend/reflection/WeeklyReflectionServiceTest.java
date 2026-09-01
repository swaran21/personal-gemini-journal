package com.pm.personalgeminijournalbackend.reflection;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.journal.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WeeklyReflectionServiceTest {
    @Test void usesAuthenticatedUidAndExactlyOneCalendarWeek() {
        JournalRepository repository = mock(JournalRepository.class); GenerativeAiService ai = mock(GenerativeAiService.class);
        JournalEntry entry = new JournalEntry("id", "I shipped the feature", "Great", Instant.EPOCH, List.of(), JournalEntry.ProcessingStatus.COMPLETED, null);
        when(repository.entriesBetween(eq("owner"), any(), any(), eq(100))).thenReturn(List.of(entry));
        when(ai.generateWeeklyReflection(List.of(entry))).thenReturn(new WeeklyReflection(null, null, 1, List.of("Consistent work"), List.of("Shipped"), List.of(), "Rest", Instant.EPOCH));
        WeeklyReflection result = new WeeklyReflectionService(repository, ai).generate("owner", new WeeklyReflectionRequest(LocalDate.of(2026, 8, 27), "UTC"));
        assertEquals(LocalDate.of(2026, 8, 24), result.weekStart()); assertEquals(LocalDate.of(2026, 8, 30), result.weekEnd()); assertEquals(1, result.entryCount());
        verify(repository).entriesBetween("owner", Instant.parse("2026-08-24T00:00:00Z"), Instant.parse("2026-08-31T00:00:00Z"), 100);
        verify(repository, never()).entriesBetween(eq("other-user"), any(), any(), anyInt());
    }

    @Test void emptyWeekSkipsAiAndReturnsHonestEmptyState() {
        JournalRepository repository = mock(JournalRepository.class); GenerativeAiService ai = mock(GenerativeAiService.class);
        when(repository.entriesBetween(eq("owner"), any(), any(), eq(100))).thenReturn(List.of());
        WeeklyReflection result = new WeeklyReflectionService(repository, ai).generate("owner", new WeeklyReflectionRequest(LocalDate.of(2026, 8, 24), "UTC"));
        assertEquals(0, result.entryCount()); assertTrue(result.highlights().isEmpty()); verifyNoInteractions(ai);
    }

    @Test void rejectsInvalidTimezoneAndFutureWeek() {
        WeeklyReflectionService service = new WeeklyReflectionService(mock(JournalRepository.class), mock(GenerativeAiService.class));
        assertThrows(IllegalArgumentException.class, () -> service.generate("owner", new WeeklyReflectionRequest(null, "Not/AZone")));
        assertThrows(IllegalArgumentException.class, () -> service.generate("owner", new WeeklyReflectionRequest(LocalDate.now().plusWeeks(2), "UTC")));
    }
}
