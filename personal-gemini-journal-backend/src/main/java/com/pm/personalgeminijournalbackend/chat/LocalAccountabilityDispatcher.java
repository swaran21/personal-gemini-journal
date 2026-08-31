package com.pm.personalgeminijournalbackend.chat;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Local entry persistence writes an outbox record in the same transaction as the journal row.
 * The dispatcher therefore deliberately performs no second enqueue operation.
 */
@Service
@Profile("local")
public class LocalAccountabilityDispatcher implements AccountabilityDispatcher {
    @Override
    public void dispatch(String uid, String entryId, String entry, Instant createdAt) {
        // Atomically enqueued by JdbcJournalRepository.saveEntry.
    }
}
