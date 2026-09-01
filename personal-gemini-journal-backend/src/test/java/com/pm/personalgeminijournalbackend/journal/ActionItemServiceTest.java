package com.pm.personalgeminijournalbackend.journal;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActionItemServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Test void sanitizesAndCreatesPendingGoalForAuthenticatedOwner() {
        JournalRepository repository = mock(JournalRepository.class);
        ActionItem expected = new ActionItem("id", "Practice Java daily", ActionItem.Status.PENDING, NOW);
        when(repository.createActionItem("owner", "Practice Java daily", NOW)).thenReturn(expected);

        ActionItem result = new ActionItemService(repository, Clock.fixed(NOW, ZoneOffset.UTC)).create("owner", " Practice\u0000   Java daily ");

        assertEquals(expected, result);
        verify(repository).createActionItem("owner", "Practice Java daily", NOW);
        verify(repository, never()).createActionItem(eq("other-user"), anyString(), any());
    }

    @Test void rejectsBlankAndOversizedGoalsBeforePersistence() {
        JournalRepository repository = mock(JournalRepository.class);
        ActionItemService service = new ActionItemService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> service.create("owner", " \u0000 "));
        assertThrows(IllegalArgumentException.class, () -> service.create("owner", "x".repeat(1001)));
        verifyNoInteractions(repository);
    }
}
