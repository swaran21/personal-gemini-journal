package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

/** Persists extracted goals off the request thread after the entry is durable. */
@Service
public class AccountabilityService {
    private final JournalRepository repository;
    public AccountabilityService(JournalRepository repository) { this.repository = repository; }
    @Async public void persist(String uid, List<String> goals, Instant createdAt) { repository.saveActionItems(uid, goals, createdAt); }
}
