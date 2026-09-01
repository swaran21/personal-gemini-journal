package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.gemini.EmbeddingService;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Processes the local transactional outbox with bounded retries and idempotent action-item writes. */
@Service
@Profile("local")
public class LocalAccountabilityWorker {
    private static final Logger log = LoggerFactory.getLogger(LocalAccountabilityWorker.class);
    private final LocalAccountabilityOutboxRepository outbox;
    private final JournalRepository journalRepository;
    private final GenerativeAiService ai;
    private final EmbeddingService embeddings;
    private final int batchSize;
    private final int maxAttempts;

    public LocalAccountabilityWorker(
            LocalAccountabilityOutboxRepository outbox,
            JournalRepository journalRepository,
            GenerativeAiService ai,
            EmbeddingService embeddings,
            @Value("${app.accountability.batch-size:5}") int batchSize,
            @Value("${app.accountability.max-attempts:5}") int maxAttempts) {
        this.outbox = outbox;
        this.journalRepository = journalRepository;
        this.ai = ai;
        this.embeddings = embeddings;
        this.batchSize = Math.max(1, Math.min(batchSize, 25));
        this.maxAttempts = Math.max(1, Math.min(maxAttempts, 10));
    }

    @Scheduled(
            initialDelayString = "${app.accountability.initial-delay-ms:2000}",
            fixedDelayString = "${app.accountability.poll-delay-ms:2000}")
    public void processAvailable() {
        for (int index = 0; index < batchSize; index++) {
            var claimed = outbox.claimNext();
            if (claimed.isEmpty()) return;
            process(claimed.orElseThrow());
        }
    }

    void process(LocalAccountabilityOutboxRepository.Job job) {
        try {
            LocalAccountabilityOutboxRepository.EntryPayload payload = outbox.entryContent(job);
            String entry = payload.content();
            var history = journalRepository.recentEntries(job.uid(), 11).stream()
                    .filter(item -> !item.id().equals(job.entryId().toString()))
                    .limit(10)
                    .toList();
            var reflection = ai.reflect(entry, history);
            List<Double> vector;
            try { vector = embeddings.embed(payload.embeddingText()); }
            catch (RuntimeException embeddingFailure) { log.warn("Embedding unavailable for job {}; reflection will still be saved", job.id()); vector = null; }
            journalRepository.completeEntryProcessing(job.uid(), job.entryId().toString(), reflection.reply(), vector);
            journalRepository.saveActionItems(job.uid(), job.entryId().toString(), reflection.actionItems(), Instant.now());
            outbox.markSucceeded(job);
        } catch (RuntimeException failure) {
            log.warn("Local accountability job {} failed on attempt {}", job.id(), job.attempt(), failure);
            outbox.markFailed(job, failure, maxAttempts);
            if (job.attempt() >= maxAttempts) {
                journalRepository.failEntryProcessing(job.uid(), job.entryId().toString(), "AI processing is temporarily unavailable. You can retry later.");
            }
        }
    }
}
