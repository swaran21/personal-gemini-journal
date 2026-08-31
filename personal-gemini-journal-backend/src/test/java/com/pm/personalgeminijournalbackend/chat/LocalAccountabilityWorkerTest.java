package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
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
    private final LocalAccountabilityWorker worker = new LocalAccountabilityWorker(outbox, journals, ai, 5, 3);

    @Test
    void stopsPollingWhenNoWorkIsAvailable() {
        when(outbox.claimNext()).thenReturn(Optional.empty());

        worker.processAvailable();

        verify(outbox).claimNext();
        verifyNoInteractions(journals, ai);
    }

    @Test
    void savesGoalsIdempotentlyUnderTheClaimedOwnerAndCompletesJob() {
        var job = job(1);
        when(outbox.claimNext()).thenReturn(Optional.of(job), Optional.empty());
        when(outbox.entryContent(job)).thenReturn("I will finish the portfolio");
        when(ai.extractActionItems(any())).thenReturn(List.of("Finish the portfolio"));

        worker.processAvailable();

        verify(journals).saveActionItems(eq("uid-1"), eq(job.entryId().toString()), eq(List.of("Finish the portfolio")), any());
        verify(outbox).markSucceeded(job);
        verify(outbox, never()).markFailed(any(), any(), anyInt());
    }

    @Test
    void returnsFailuresToTheOutboxRetryPolicy() {
        var job = job(2);
        when(outbox.entryContent(job)).thenReturn("entry");
        when(ai.extractActionItems("entry")).thenThrow(new IllegalStateException("model unavailable"));

        worker.process(job);

        verify(outbox).markFailed(eq(job), any(IllegalStateException.class), eq(3));
        verify(outbox, never()).markSucceeded(job);
        verifyNoInteractions(journals);
    }

    private LocalAccountabilityOutboxRepository.Job job(int attempt) {
        return new LocalAccountabilityOutboxRepository.Job(UUID.randomUUID(), "uid-1", UUID.randomUUID(), attempt);
    }
}
