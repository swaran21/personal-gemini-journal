package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.List;

/** Persists extracted goals off the request thread after the entry is durable. */
@Service
public class AccountabilityService {
    private static final Logger log = LoggerFactory.getLogger(AccountabilityService.class);
    private final JournalRepository repository;
    public AccountabilityService(JournalRepository repository) { this.repository = repository; }
    @Async("accountabilityExecutor")
    public void persist(String uid, List<String> goals, Instant createdAt) {
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
