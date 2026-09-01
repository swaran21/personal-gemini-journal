package com.pm.personalgeminijournalbackend.journal;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Repository
@Profile("local")
public class JdbcJournalRepository implements JournalRepository {
    private final JdbcClient jdbc;
    public JdbcJournalRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override @Transactional public String createPendingEntry(String uid, String text, GeoLocation location, Instant now) {
        scope(uid);
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO journal_entries(id,user_id,content,processing_status,created_at,latitude,longitude,location_label) VALUES (:id,:uid,:content,'PENDING',:createdAt,:latitude,:longitude,:label)")
                .param("id", id).param("uid", uid).param("content", text).param("createdAt", Timestamp.from(now))
                .param("latitude", location == null ? null : location.latitude()).param("longitude", location == null ? null : location.longitude()).param("label", location == null ? null : location.label()).update();
        jdbc.sql("INSERT INTO accountability_outbox(id,user_id,journal_entry_id,status,attempts,available_at,created_at) VALUES (:id,:uid,:entryId,'PENDING',0,:createdAt,:createdAt)")
                .param("id", UUID.randomUUID()).param("uid", uid).param("entryId", id).param("createdAt", Timestamp.from(now)).update();
        return id.toString();
    }

    @Override @Transactional public void completeEntryProcessing(String uid, String entryId, String reply, List<Double> embedding) {
        scope(uid);
        int changed = jdbc.sql("UPDATE journal_entries SET ai_response=:reply,embedding=CAST(:embedding AS vector),processing_status='COMPLETED',processing_error=NULL,version=version+1 WHERE id=:id AND user_id=:uid")
                .param("reply", reply).param("embedding", embedding == null || embedding.isEmpty() ? null : vector(embedding)).param("id", uuid(entryId)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Journal entry not found");
    }

    @Override @Transactional public void failEntryProcessing(String uid, String entryId, String safeError) {
        scope(uid);
        int changed = jdbc.sql("UPDATE journal_entries SET processing_status='FAILED',processing_error=:error,version=version+1 WHERE id=:id AND user_id=:uid")
                .param("error", safeError).param("id", uuid(entryId)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Journal entry not found");
    }

    @Override @Transactional public JournalEntry retryEntryProcessing(String uid, String entryId, Instant now) {
        scope(uid);
        UUID id = uuid(entryId);
        int changed = jdbc.sql("UPDATE journal_entries SET processing_status='PENDING',processing_error=NULL,version=version+1 WHERE id=:id AND user_id=:uid AND processing_status='FAILED'")
                .param("id", id).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Failed journal entry not found");
        jdbc.sql("UPDATE accountability_outbox SET status='PENDING',attempts=0,available_at=:availableAt,locked_at=NULL,last_error=NULL,completed_at=NULL WHERE journal_entry_id=:entryId AND user_id=:uid")
                .param("availableAt", Timestamp.from(now)).param("entryId", id).param("uid", uid).update();
        return jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE id=:id AND user_id=:uid")
                .param("id", id).param("uid", uid).query((rs, row) -> entry(rs, List.of())).single();
    }

    @Override @Transactional public JournalEntry updateEntryContent(String uid, String entryId, String text, GeoLocation location, Instant now) {
        scope(uid); UUID id = uuid(entryId);
        int changed = jdbc.sql("UPDATE journal_entries SET content=:content,ai_response=NULL,embedding=NULL,processing_status='PENDING',processing_error=NULL,latitude=:latitude,longitude=:longitude,location_label=:label,version=version+1 WHERE id=:id AND user_id=:uid")
                .param("content", text).param("latitude", location == null ? null : location.latitude()).param("longitude", location == null ? null : location.longitude()).param("label", location == null ? null : location.label()).param("id", id).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Journal entry not found");
        jdbc.sql("UPDATE accountability_outbox SET status='PENDING',attempts=0,available_at=:availableAt,locked_at=NULL,last_error=NULL,completed_at=NULL WHERE journal_entry_id=:entryId AND user_id=:uid")
                .param("availableAt", Timestamp.from(now)).param("entryId", id).param("uid", uid).update();
        return jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE id=:id AND user_id=:uid")
                .param("id", id).param("uid", uid).query((rs, row) -> entry(rs, List.of())).single();
    }

    @Override @Transactional public void deleteJournalEntry(String uid, String entryId) {
        scope(uid);
        int changed = jdbc.sql("DELETE FROM journal_entries WHERE id=:id AND user_id=:uid").param("id", uuid(entryId)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Journal entry not found");
    }

    @Override @Transactional public void saveActionItems(String uid, List<String> goals, Instant now) {
        saveActionItems(uid, null, goals, now);
    }

    @Override @Transactional public void saveActionItems(String uid, String sourceEntryId, List<String> goals, Instant now) {
        if (goals.isEmpty()) return;
        scope(uid);
        UUID sourceId = sourceEntryId == null ? null : uuid(sourceEntryId);
        for (String goal : goals) jdbc.sql("INSERT INTO action_items(id,user_id,source_entry_id,goal,completed,status,created_at) VALUES (:id,:uid,:sourceId,:goal,false,'PROPOSED',:createdAt) ON CONFLICT (user_id,source_entry_id,goal) DO NOTHING")
                .param("id", UUID.randomUUID()).param("uid", uid).param("sourceId", sourceId).param("goal", goal).param("createdAt", Timestamp.from(now)).update();
    }

    @Override @Transactional public ActionItem createActionItem(String uid, String goal, Instant now) {
        scope(uid);
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO action_items(id,user_id,source_entry_id,goal,completed,status,created_at) VALUES (:id,:uid,NULL,:goal,false,'PENDING',:createdAt)")
                .param("id", id).param("uid", uid).param("goal", goal).param("createdAt", Timestamp.from(now)).update();
        return new ActionItem(id.toString(), goal, ActionItem.Status.PENDING, now);
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> recentEntries(String uid, int maxResults) {
        scope(uid);
        return jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE user_id=:uid ORDER BY created_at DESC,id DESC LIMIT :limit")
                .param("uid", uid).param("limit", bounded(maxResults)).query((rs, row) -> entry(rs, List.of())).list();
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> entriesBetween(String uid, Instant startInclusive, Instant endExclusive, int maxResults) {
        scope(uid);
        return jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE user_id=:uid AND created_at>=:start AND created_at<:end ORDER BY created_at ASC,id ASC LIMIT :limit")
                .param("uid", uid).param("start", Timestamp.from(startInclusive)).param("end", Timestamp.from(endExclusive)).param("limit", bounded(maxResults)).query((rs, row) -> entry(rs, List.of())).list();
    }

    @Override @Transactional(readOnly = true) public PageSlice<JournalEntry> listEntries(String uid, int limit, String cursor) {
        scope(uid); int pageSize = bounded(limit); PageCursor.Decoded decoded = PageCursor.decode(cursor);
        String condition = decoded == null ? "" : " AND (created_at,id)<(:cursorAt,:cursorId)";
        var query = jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE user_id=:uid" + condition + " ORDER BY created_at DESC,id DESC LIMIT :limit")
                .param("uid", uid).param("limit", pageSize + 1);
        if (decoded != null) query = query.param("cursorAt", Timestamp.from(decoded.createdAt())).param("cursorId", uuid(decoded.id()));
        List<JournalEntry> rows = query.query((rs, row) -> entry(rs, List.of())).list();
        return page(rows, pageSize);
    }

    @Override @Transactional(readOnly = true) public PageSlice<ActionItem> listActionItems(String uid, int limit, String cursor) {
        scope(uid); int pageSize = bounded(limit); PageCursor.Decoded decoded = PageCursor.decode(cursor);
        String condition = decoded == null ? "" : " AND (created_at,id)<(:cursorAt,:cursorId)";
        var query = jdbc.sql("SELECT id,goal,status,created_at FROM action_items WHERE user_id=:uid" + condition + " ORDER BY created_at DESC,id DESC LIMIT :limit")
                .param("uid", uid).param("limit", pageSize + 1);
        if (decoded != null) query = query.param("cursorAt", Timestamp.from(decoded.createdAt())).param("cursorId", uuid(decoded.id()));
        List<ActionItem> rows = query.query((rs, row) -> new ActionItem(rs.getObject("id", UUID.class).toString(), rs.getString("goal"), ActionItem.Status.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant())).list();
        return page(rows, pageSize);
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> findRelevant(String uid, List<Double> queryEmbedding, int limit) {
        scope(uid);
        return jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE user_id=:uid AND processing_status='COMPLETED' AND embedding IS NOT NULL ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit")
                .param("uid", uid).param("embedding", vector(queryEmbedding)).param("limit", Math.max(1, Math.min(limit, 10)))
                .query((rs, row) -> entry(rs, List.of())).list();
    }

    @Override @Transactional(readOnly = true) public List<JournalEntry> findTextRelevant(String uid, String query, int limit) {
        scope(uid);
        List<String> terms = Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(term -> term.length() > 2).filter(term -> !TEXT_STOP_WORDS.contains(term)).distinct().toList();
        if (terms.isEmpty()) return List.of();
        return jdbc.sql("SELECT id,content,ai_response,created_at,processing_status,processing_error,latitude,longitude,location_label FROM journal_entries WHERE user_id=:uid ORDER BY created_at DESC,id DESC LIMIT 100")
                .param("uid", uid).query((rs, row) -> entry(rs, List.of())).list().stream()
                .filter(entry -> terms.stream().anyMatch(term -> searchable(entry).contains(term)))
                .limit(Math.max(1, Math.min(limit, 10))).toList();
    }

    @Override @Transactional public void setActionItemStatus(String uid, String id, ActionItem.Status status) {
        scope(uid);
        int changed = jdbc.sql("UPDATE action_items SET status=:status,completed=:completed,version=version+1 WHERE id=:id AND user_id=:uid")
                .param("status", status.name()).param("completed", status == ActionItem.Status.COMPLETED).param("id", uuid(id)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Action item not found");
    }

    @Override @Transactional public void deleteActionItem(String uid, String id) {
        scope(uid);
        int changed = jdbc.sql("DELETE FROM action_items WHERE id=:id AND user_id=:uid").param("id", uuid(id)).param("uid", uid).update();
        if (changed == 0) throw new NoSuchElementException("Action item not found");
    }

    @Override @Transactional public void deleteAllUserData(String uid) {
        scope(uid);
        jdbc.sql("DELETE FROM action_items WHERE user_id=:uid").param("uid", uid).update();
        // journal_entries cascades to the internal processing outbox.
        jdbc.sql("DELETE FROM journal_entries WHERE user_id=:uid").param("uid", uid).update();
    }

    private void scope(String uid) { jdbc.sql("SELECT set_config('app.current_user_id', :uid, true)").param("uid", uid).query(String.class).single(); }
    private int bounded(int value) { return Math.max(1, Math.min(value, 100)); }
    private UUID uuid(String value) { try { return UUID.fromString(value); } catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid document id"); } }
    private String vector(List<Double> values) { if (values == null || values.isEmpty()) throw new IllegalArgumentException("Embedding must not be empty"); return values.toString(); }
    private static final java.util.Set<String> TEXT_STOP_WORDS = java.util.Set.of("what", "when", "where", "which", "with", "from", "about", "have", "this", "that", "were", "did", "near", "into", "your", "mine");
    private String searchable(JournalEntry entry) { return (entry.text() + " " + (entry.location() == null ? "" : entry.location().label())).toLowerCase(Locale.ROOT); }
    private JournalEntry entry(java.sql.ResultSet rs, List<Double> embedding) throws java.sql.SQLException {
        return new JournalEntry(
                rs.getObject("id", UUID.class).toString(), rs.getString("content"), rs.getString("ai_response"),
                rs.getTimestamp("created_at").toInstant(), embedding,
                JournalEntry.ProcessingStatus.valueOf(rs.getString("processing_status")), rs.getString("processing_error"),
                rs.getObject("latitude") == null ? null : new GeoLocation(rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("location_label")));
    }
    private <T> PageSlice<T> page(List<T> rows, int limit) {
        boolean hasMore = rows.size() > limit; List<T> items = hasMore ? rows.subList(0, limit) : rows;
        if (items.isEmpty()) return new PageSlice<>(List.of(), null, false);
        Object last = items.get(items.size() - 1);
        Instant createdAt = last instanceof JournalEntry entry ? entry.createdAt() : ((ActionItem) last).createdAt();
        String id = last instanceof JournalEntry entry ? entry.id() : ((ActionItem) last).id();
        return new PageSlice<>(items, PageCursor.encode(createdAt, id), hasMore);
    }
}
