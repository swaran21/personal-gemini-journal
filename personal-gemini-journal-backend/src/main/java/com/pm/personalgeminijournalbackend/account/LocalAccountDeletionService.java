package com.pm.personalgeminijournalbackend.account;

import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Local Keycloak owns identity lifecycle; this service removes all application data transactionally. */
@Service
@Profile("local")
public class LocalAccountDeletionService implements AccountDeletionService {
    private final JournalRepository repository;
    public LocalAccountDeletionService(JournalRepository repository) { this.repository = repository; }
    @Override public DeletionResult delete(String uid) {
        repository.deleteAllUserData(uid);
        return new DeletionResult(true, false, "KEYCLOAK");
    }
}
