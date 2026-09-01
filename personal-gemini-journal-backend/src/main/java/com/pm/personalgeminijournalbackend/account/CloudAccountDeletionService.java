package com.pm.personalgeminijournalbackend.account;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Deletes user data first, then the Firebase identity so a partial failure remains safely retryable. */
@Service
@Profile("cloud")
public class CloudAccountDeletionService implements AccountDeletionService {
    private final JournalRepository repository;
    private final FirebaseAuth firebaseAuth;
    public CloudAccountDeletionService(JournalRepository repository, FirebaseAuth firebaseAuth) { this.repository = repository; this.firebaseAuth = firebaseAuth; }
    @Override public DeletionResult delete(String uid) {
        repository.deleteAllUserData(uid);
        try {
            firebaseAuth.deleteUser(uid);
            return new DeletionResult(true, true, "FIREBASE");
        } catch (FirebaseAuthException exception) {
            throw new IllegalStateException("Identity deletion failed after application data deletion", exception);
        }
    }
}
