package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GeminiService;
import com.pm.personalgeminijournalbackend.gemini.EmbeddingService;
import com.pm.personalgeminijournalbackend.gemini.GeminiResult;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

class AccountabilityServiceTest {
    private final JournalRepository repository = mock(JournalRepository.class);
    private final GeminiService gemini = mock(GeminiService.class);
    private final EmbeddingService embeddings = mock(EmbeddingService.class);
    private final AccountabilityService service = new AccountabilityService(repository, gemini, embeddings);

    private void aiSucceeds(List<String> goals) {
        when(repository.recentEntries(anyString(), eq(11))).thenReturn(List.of());
        when(gemini.reflect(anyString(), anyList())).thenReturn(new GeminiResult("Reflective reply", List.of()));
        when(embeddings.embed(anyString())).thenReturn(List.of(1d));
        when(gemini.extractActionItems(anyString())).thenReturn(goals);
    }

    @Test void doesNotWriteWhenGeminiFindsNoGoals() {
        aiSucceeds(List.of());
        service.dispatch("uid", "entry-id", "reflection", Instant.EPOCH);
        verify(repository).completeEntryProcessing("uid", "entry-id", "Reflective reply", List.of(1d));
        verify(repository).saveActionItems("uid", "entry-id", List.of(), Instant.EPOCH);
    }

    @Test void persistsGoalsUnderTheSuppliedUid() {
        aiSucceeds(List.of("Finish portfolio"));
        service.dispatch("uid-1", "entry-id", "reflection", Instant.EPOCH);
        verify(repository).saveActionItems("uid-1", "entry-id", List.of("Finish portfolio"), Instant.EPOCH);
    }

    @Test void retriesPersistenceThenSucceeds() {
        aiSucceeds(List.of("Finish portfolio"));
        doThrow(new IllegalStateException("temporary")).doNothing().when(repository).completeEntryProcessing(eq("uid"), eq("entry-id"), anyString(), anyList());
        service.dispatch("uid", "entry-id", "reflection", Instant.EPOCH);
        verify(repository, times(2)).completeEntryProcessing(eq("uid"), eq("entry-id"), anyString(), anyList());
    }

    @Test void stopsAfterThreePersistenceFailures() {
        aiSucceeds(List.of("Finish portfolio"));
        doThrow(new IllegalStateException("down")).when(repository).completeEntryProcessing(eq("uid"), eq("entry-id"), anyString(), anyList());
        service.dispatch("uid", "entry-id", "reflection", Instant.EPOCH);
        verify(repository, times(3)).completeEntryProcessing(eq("uid"), eq("entry-id"), anyString(), anyList());
        verify(repository).failEntryProcessing(eq("uid"), eq("entry-id"), anyString());
    }
}
