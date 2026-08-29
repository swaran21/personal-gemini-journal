package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GeminiService;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

class AccountabilityServiceTest {
    private final JournalRepository repository = mock(JournalRepository.class);
    private final GeminiService gemini = mock(GeminiService.class);
    private final AccountabilityService service = new AccountabilityService(repository, gemini);

    @Test void doesNotWriteWhenGeminiFindsNoGoals() {
        when(gemini.extractActionItems("reflection")).thenReturn(List.of());
        service.extractAndPersist("uid", "reflection", Instant.EPOCH);
        verifyNoInteractions(repository);
    }

    @Test void persistsGoalsUnderTheSuppliedUid() {
        when(gemini.extractActionItems("reflection")).thenReturn(List.of("Finish portfolio"));
        service.extractAndPersist("uid-1", "reflection", Instant.EPOCH);
        verify(repository).saveActionItems("uid-1", List.of("Finish portfolio"), Instant.EPOCH);
    }

    @Test void retriesPersistenceThenSucceeds() {
        when(gemini.extractActionItems("reflection")).thenReturn(List.of("Finish portfolio"));
        doThrow(new IllegalStateException("temporary")).doNothing().when(repository).saveActionItems(eq("uid"), anyList(), eq(Instant.EPOCH));
        service.extractAndPersist("uid", "reflection", Instant.EPOCH);
        verify(repository, times(2)).saveActionItems("uid", List.of("Finish portfolio"), Instant.EPOCH);
    }

    @Test void stopsAfterThreePersistenceFailures() {
        when(gemini.extractActionItems("reflection")).thenReturn(List.of("Finish portfolio"));
        doThrow(new IllegalStateException("down")).when(repository).saveActionItems(eq("uid"), anyList(), eq(Instant.EPOCH));
        service.extractAndPersist("uid", "reflection", Instant.EPOCH);
        verify(repository, times(3)).saveActionItems("uid", List.of("Finish portfolio"), Instant.EPOCH);
    }
}
