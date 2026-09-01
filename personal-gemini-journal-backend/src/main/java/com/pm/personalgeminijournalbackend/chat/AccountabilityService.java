package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.gemini.EmbeddingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.List;

/** Persists extracted goals off the request thread after the entry is durable. */
@Service
@Profile("cloud")
public class AccountabilityService implements AccountabilityDispatcher {
    private static final Logger log = LoggerFactory.getLogger(AccountabilityService.class);
    private final JournalRepository repository;
    private final GenerativeAiService gemini;
    private final EmbeddingService embeddings;
    public AccountabilityService(JournalRepository repository, GenerativeAiService gemini, EmbeddingService embeddings) { this.repository = repository; this.gemini = gemini; this.embeddings = embeddings; }
    @Async("accountabilityExecutor")
    public void dispatch(String uid, String entryId, String entry, Instant createdAt) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                var history = repository.recentEntries(uid, 11).stream().filter(item -> !item.id().equals(entryId)).limit(10).toList();
                var reflection = gemini.reflect(entry, history);
                List<Double> vector;
                try { vector = embeddings.embed(entry); }
                catch (RuntimeException embeddingFailure) { log.warn("Embedding unavailable for entry {}; reflection will still be saved", entryId); vector = null; }
                repository.completeEntryProcessing(uid, entryId, reflection.reply(), vector);
                repository.saveActionItems(uid, entryId, reflection.actionItems(), createdAt);
                return;
            } catch (RuntimeException exception) {
                if (attempt == 3) {
                    log.error("Could not process journal entry after {} attempts", attempt, exception);
                    repository.failEntryProcessing(uid, entryId, "AI processing is temporarily unavailable. You can retry later.");
                    return;
                }
                log.warn("Action-item persistence failed; retrying (attempt {}/3)", attempt, exception);
                try { Thread.sleep(250L * attempt); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
        }
    }
}
