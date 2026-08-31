package com.pm.personalgeminijournalbackend.journal;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Repository
@Profile("local")
public class JdbcJournalRepository implements JournalRepository {
    private final JdbcClient jdbc;
    public JdbcJournalRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override @Transactional public String saveEntry(String uid, String text, String reply, List<Double> embedding, Instant now) {
        scope(uid);
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO journal_entries(id,user_id,content,ai_response,embedding,created_at) VALUES (:id,:uid,:content,:reply,CAST(:embedding AS vector),:createdAt)")
                .param("id", id).param("uid", uid).param("content", text).param("reply", reply).param("embedding", vector(embedding)).param("createdAt", Timestamp.from(now)).update();
        jdbc.sql("INSERT INTO accountability_outbox(id,user_id,journal_entry_id,status,attempts,available_at,created_at) VALUES (:id,:uid,:entryId,'PENDING',0,:createdAt,:createdAt)")
                .param("id", UUID.randomUUID()).param("uid", uid).param("entryId", id).param("createdAt", Timestamp.from(now)).update();
        return id.toString();
    }

    @Override @Transactional public void saveActionItems(String uid, List<String> goals, Instant now) {
        saveActionItems(uid, null, goals, now);
    }

    @Override @Transactional public void saveActionItems(String uid, String sourceEntryId, List<String> goals, Instant now) {
        if (goals.isEmpty()) return;
        scope(uid);
        UUID sourceId = sourceEntryId == null ? null : uuid(sourceEntryId);
        for (String goal : goals) jdbc.sql("INSERT INTO action_items(id,user_id,source_entry_id,goal,completed,created_at) VALUES (:id,:uid,:sourceId,:goal,false,:createdAt) ON CONFLICT (user_id,source_entry_id,goal) DO NOTHING")
                .param("id", UUID.randomUUID()).param("uid", uid).param("sourceId", sourceId).param("goal", goal).param("createdAt", Timestamp.from(now)).update();
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> recentEntries(String uid, int maxResults) {
        scope(uid);
        return jdbc.sql("SELECT id,content,ai_response,created_at FROM journal_entries WHERE user_id=:uid ORDER BY created_at DESC LIMIT :limit")
                .param("uid", uid).param("limit", bounded(maxResults)).query((rs, row) -> new JournalEntry(rs.getObject("id", UUID.class).toString(), rs.getString("content"), rs.getString("ai_response"), rs.getTimestamp("created_at").toInstant(), List.of())).list();
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> listEntries(String uid) { return recentEntries(uid, 100); }

    @Override @Transactional(readOnly = true) public List<ActionItem> listActionItems(String uid) {
        scope(uid);
        return jdbc.sql("SELECT id,goal,completed,created_at FROM action_items WHERE user_id=:uid ORDER BY created_at DESC LIMIT 100")
                .param("uid", uid).query((rs, row) -> new ActionItem(rs.getObject("id", UUID.class).toString(), rs.getString("goal"), rs.getBoolean("completed"), rs.getTimestamp("created_at").toInstant())).list();
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> findRelevant(String uid, List<Double> queryEmbedding, int limit) {
        scope(uid);
        return jdbc.sql("SELECT id,content,ai_response,created_at FROM journal_entries WHERE user_id=:uid AND embedding IS NOT NULL ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit")
                .param("uid", uid).param("embedding", vector(queryEmbedding)).param("limit", Math.max(1, Math.min(limit, 10)))
                .query((rs, row) -> new JournalEntry(rs.getObject("id", UUID.class).toString(), rs.getString("content"), rs.getString("ai_response"), rs.getTimestamp("created_at").toInstant(), List.of())).list();
    }

    @Override @Transactional public void setActionItemCompleted(String uid, String id, boolean completed) {
        scope(uid);
        int changed = jdbc.sql("UPDATE action_items SET completed=:completed WHERE id=:id AND user_id=:uid")
                .param("completed", completed).param("id", uuid(id)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Action item not found");
    }

    @Override @Transactional public void deleteActionItem(String uid, String id) {
        scope(uid);
        int changed = jdbc.sql("DELETE FROM action_items WHERE id=:id AND user_id=:uid").param("id", uuid(id)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Action item not found");
    }

    private void scope(String uid) { jdbc.sql("SELECT set_config('app.current_user_id', :uid, true)").param("uid", uid).query(String.class).single(); }
    private int bounded(int value) { return Math.max(1, Math.min(value, 100)); }
    private UUID uuid(String value) { try { return UUID.fromString(value); } catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid document id"); } }
    private String vector(List<Double> values) { if (values == null || values.isEmpty()) throw new IllegalArgumentException("Embedding must not be empty"); return values.toString(); }
}
