package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.gemini.EmbeddingService;
import com.pm.personalgeminijournalbackend.gemini.GeminiResult;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LocalAccountabilityWorkerTest {
    private final LocalAccountabilityOutboxRepository outbox = mock(LocalAccountabilityOutboxRepository.class);
    private final JournalRepository journals = mock(JournalRepository.class);
    private final GenerativeAiService ai = mock(GenerativeAiService.class);
    private final EmbeddingService embeddings = mock(EmbeddingService.class);
    private final LocalAccountabilityWorker worker = new LocalAccountabilityWorker(outbox, journals, ai, embeddings, 5, 3);

    @Test
    void stopsPollingWhenNoWorkIsAvailable() {
        when(outbox.claimNext()).thenReturn(Optional.empty());

        worker.processAvailable();

        verify(outbox).claimNext();
        verifyNoInteractions(journals, ai, embeddings);
    }

    @Test
    void savesGoalsIdempotentlyUnderTheClaimedOwnerAndCompletesJob() {
        var job = job(1);
        when(outbox.claimNext()).thenReturn(Optional.of(job), Optional.empty());
        when(outbox.entryContent(job)).thenReturn(new LocalAccountabilityOutboxRepository.EntryPayload("I will finish the portfolio", "I will finish the portfolio"));
        when(journals.recentEntries("uid-1", 11)).thenReturn(List.of());
        when(ai.reflect(any(), anyList())).thenReturn(new GeminiResult("You have a clear next step.", List.of("Finish the portfolio")));
        when(embeddings.embed(any())).thenReturn(List.of(1d));

        worker.processAvailable();

        verify(journals).completeEntryProcessing("uid-1", job.entryId().toString(), "You have a clear next step.", List.of(1d));
        verify(journals).saveActionItems(eq("uid-1"), eq(job.entryId().toString()), eq(List.of("Finish the portfolio")), any());
        verify(ai, never()).extractActionItems(anyString());
        verify(outbox).markSucceeded(job);
        verify(outbox, never()).markFailed(any(), any(), anyInt());
    }

    @Test
    void returnsFailuresToTheOutboxRetryPolicy() {
        var job = job(2);
        when(outbox.entryContent(job)).thenReturn(new LocalAccountabilityOutboxRepository.EntryPayload("entry", "entry"));
        when(journals.recentEntries("uid-1", 11)).thenReturn(List.of());
        when(ai.reflect(eq("entry"), anyList())).thenThrow(new IllegalStateException("model unavailable"));

        worker.process(job);

        verify(outbox).markFailed(eq(job), any(IllegalStateException.class), eq(3));
        verify(outbox, never()).markSucceeded(job);
        verify(journals).recentEntries("uid-1", 11);
        verify(journals, never()).completeEntryProcessing(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void savesProposalsReturnedWithTheSuccessfulReflection() {
        var job = job(1);
        when(outbox.entryContent(job)).thenReturn(new LocalAccountabilityOutboxRepository.EntryPayload("entry", "entry"));
        when(journals.recentEntries("uid-1", 11)).thenReturn(List.of());
        when(ai.reflect(eq("entry"), anyList())).thenReturn(new GeminiResult("reply", List.of("Write the security tests")));
        when(embeddings.embed("entry")).thenReturn(List.of(1d));

        worker.process(job);

        verify(journals).completeEntryProcessing("uid-1", job.entryId().toString(), "reply", List.of(1d));
        verify(journals).saveActionItems(eq("uid-1"), eq(job.entryId().toString()), eq(List.of("Write the security tests")), any());
        verify(outbox).markSucceeded(job);
        verify(outbox, never()).markFailed(any(), any(), anyInt());
    }

    @Test
    void embedsApprovedLocationLabelAlongsideEntryText() {
        var job = job(1);
        when(outbox.entryContent(job)).thenReturn(new LocalAccountabilityOutboxRepository.EntryPayload("coffee", "coffee\nLocation: Cubbon Park"));
        when(journals.recentEntries("uid-1", 11)).thenReturn(List.of());
        when(ai.reflect(eq("coffee"), anyList())).thenReturn(new GeminiResult("reply", List.of()));
        when(embeddings.embed("coffee\nLocation: Cubbon Park")).thenReturn(List.of(1d));
        worker.process(job);
        verify(embeddings).embed("coffee\nLocation: Cubbon Park");
    }

    @Test
    void embeddingQuotaFailureDoesNotLoseAiReflection() {
        var job = job(1);
        when(outbox.entryContent(job)).thenReturn(new LocalAccountabilityOutboxRepository.EntryPayload("entry", "entry\nLocation: Home"));
        when(journals.recentEntries("uid-1", 11)).thenReturn(List.of());
        when(ai.reflect(eq("entry"), anyList())).thenReturn(new GeminiResult("reply", List.of()));
        when(embeddings.embed(anyString())).thenThrow(new IllegalStateException("quota"));
        worker.process(job);
        verify(journals).completeEntryProcessing("uid-1", job.entryId().toString(), "reply", null);
        verify(outbox).markSucceeded(job);
    }

    private LocalAccountabilityOutboxRepository.Job job(int attempt) {
        return new LocalAccountabilityOutboxRepository.Job(UUID.randomUUID(), "uid-1", UUID.randomUUID(), attempt);
    }
}
