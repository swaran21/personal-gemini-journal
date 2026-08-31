package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
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
    public AccountabilityService(JournalRepository repository, GenerativeAiService gemini) { this.repository = repository; this.gemini = gemini; }
    @Async("accountabilityExecutor")
    public void dispatch(String uid, String entryId, String entry, Instant createdAt) {
        List<String> goals = gemini.extractActionItems(entry);
        if (goals.isEmpty()) return;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                repository.saveActionItems(uid, goals, createdAt);
                return;
            } catch (RuntimeException exception) {
                if (attempt == 3) {
                    log.error("Could not persist {} extracted action items after {} attempts", goals.size(), attempt, exception);
                    return;
                }
                log.warn("Action-item persistence failed; retrying (attempt {}/3)", attempt, exception);
                try { Thread.sleep(250L * attempt); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
        }
    }
}
