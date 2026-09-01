package com.pm.personalgeminijournalbackend.chat;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Internal-only durable queue. It has no HTTP exposure and never accepts client-supplied ownership. */
@Repository
@Profile("local")
public class LocalAccountabilityOutboxRepository {
    private final JdbcClient jdbc;

    public LocalAccountabilityOutboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Optional<Job> claimNext() {
        jdbc.sql("UPDATE accountability_outbox SET status='PENDING', locked_at=NULL WHERE status='PROCESSING' AND locked_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes'").update();
        Optional<Job> candidate = jdbc.sql("SELECT id,user_id,journal_entry_id,attempts FROM accountability_outbox WHERE status='PENDING' AND available_at <= CURRENT_TIMESTAMP ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1")
                .query((rs, rowNum) -> new Job(
                        rs.getObject("id", UUID.class),
                        rs.getString("user_id"),
                        rs.getObject("journal_entry_id", UUID.class),
                        rs.getInt("attempts") + 1))
                .optional();
        candidate.ifPresent(job -> jdbc.sql("UPDATE accountability_outbox SET status='PROCESSING',attempts=:attempts,locked_at=CURRENT_TIMESTAMP WHERE id=:id")
                .param("attempts", job.attempt()).param("id", job.id()).update());
        return candidate;
    }

    @Transactional(readOnly = true)
    public EntryPayload entryContent(Job job) {
        scope(job.uid());
        return jdbc.sql("SELECT content,location_label FROM journal_entries WHERE id=:entryId AND user_id=:uid")
                .param("entryId", job.entryId()).param("uid", job.uid()).query((rs, row) -> {
                    String content = rs.getString("content"); String label = rs.getString("location_label");
                    String embeddingText = label == null || label.isBlank() ? content : content + "\nLocation: " + label;
                    return new EntryPayload(content, embeddingText);
                }).single();
    }

    @Transactional
    public void markSucceeded(Job job) {
        jdbc.sql("UPDATE accountability_outbox SET status='SUCCEEDED',locked_at=NULL,last_error=NULL,completed_at=CURRENT_TIMESTAMP WHERE id=:id AND status='PROCESSING'")
                .param("id", job.id()).update();
    }

    @Transactional
    public void markFailed(Job job, RuntimeException failure, int maxAttempts) {
        boolean dead = job.attempt() >= maxAttempts;
        long delaySeconds = Math.min(300L, 1L << Math.min(job.attempt(), 8));
        jdbc.sql("UPDATE accountability_outbox SET status=:status,locked_at=NULL,last_error=:error,available_at=CURRENT_TIMESTAMP + make_interval(secs => :delaySeconds),completed_at=CASE WHEN :dead THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id=:id")
                .param("status", dead ? "DEAD" : "PENDING")
                .param("error", failure.getClass().getSimpleName())
                .param("delaySeconds", delaySeconds)
                .param("dead", dead)
                .param("id", job.id())
                .update();
    }

    private void scope(String uid) {
        jdbc.sql("SELECT set_config('app.current_user_id', :uid, true)").param("uid", uid).query(String.class).single();
    }

    public record Job(UUID id, String uid, UUID entryId, int attempt) { }
    public record EntryPayload(String content, String embeddingText) { }
}
