package com.pm.personalgeminijournalbackend.journal;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** All paths are constructed from the authenticated UID; no caller can pass a Firestore path. */
@Repository
public class JournalRepository {
    private final Firestore firestore;
    public JournalRepository(Firestore firestore) { this.firestore = firestore; }
    private com.google.cloud.firestore.CollectionReference entries(String uid) { return firestore.collection("users").document(uid).collection("journal_entries"); }
    private com.google.cloud.firestore.CollectionReference actionItems(String uid) { return firestore.collection("users").document(uid).collection("action_items"); }
    public String saveEntry(String uid, String text, String reply, List<Double> embedding, Instant now) {
        var ref = entries(uid).document();
        Map<String, Object> data = new HashMap<>(); data.put("text", text); data.put("response", reply); data.put("embedding", embedding); data.put("createdAt", now.toEpochMilli());
        wait(ref.set(data)); return ref.getId();
    }
    public void saveActionItems(String uid, List<String> goals, Instant now) {
        if (goals.isEmpty()) return;
        WriteBatch batch = firestore.batch();
        for (String goal : goals) batch.set(actionItems(uid).document(), Map.of("text", goal, "completed", false, "createdAt", now.toEpochMilli()));
        wait(batch.commit());
    }
    public List<JournalEntry> recentEntries(String uid, int maxResults) {
        try {
            return entries(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(maxResults).get().get().getDocuments().stream().map(d ->
                new JournalEntry(d.getId(), d.getString("text"), d.getString("response"), Instant.ofEpochMilli(Objects.requireNonNullElse(d.getLong("createdAt"), 0L)), List.of())
            ).toList();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); }
          catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); }
    }
    public List<JournalEntry> listEntries(String uid) { return recentEntries(uid, 100); }
    public List<ActionItem> listActionItems(String uid) {
        try {
            return actionItems(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get().get().getDocuments().stream().map(d ->
                    new ActionItem(d.getId(), d.getString("text"), Boolean.TRUE.equals(d.getBoolean("completed")), Instant.ofEpochMilli(Objects.requireNonNullElse(d.getLong("createdAt"), 0L)))) .toList();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); }
        catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); }
    }
    public void setActionItemCompleted(String uid, String id, boolean completed) { wait(actionItems(uid).document(validId(id)).update("completed", completed)); }
    public void deleteActionItem(String uid, String id) { wait(actionItems(uid).document(validId(id)).delete()); }
    private String validId(String id) { if (id == null || !id.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException("Invalid document id"); return id; }
    private void wait(com.google.api.core.ApiFuture<?> future) { try { future.get(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); } catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); } }
}
