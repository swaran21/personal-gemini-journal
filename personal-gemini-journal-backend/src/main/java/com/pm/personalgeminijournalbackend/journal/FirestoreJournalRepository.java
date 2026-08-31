package com.pm.personalgeminijournalbackend.journal;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
@Profile("cloud")
public class FirestoreJournalRepository implements JournalRepository {
    private final Firestore firestore;
    public FirestoreJournalRepository(Firestore firestore) { this.firestore = firestore; }
    private com.google.cloud.firestore.CollectionReference entries(String uid) { return firestore.collection("users").document(uid).collection("journal_entries"); }
    private com.google.cloud.firestore.CollectionReference actionItems(String uid) { return firestore.collection("users").document(uid).collection("action_items"); }

    @Override public String saveEntry(String uid, String text, String reply, List<Double> embedding, Instant now) {
        var ref = entries(uid).document();
        Map<String, Object> data = new HashMap<>(); data.put("text", text); data.put("response", reply); data.put("embedding", embedding); data.put("createdAt", now.toEpochMilli());
        wait(ref.set(data)); return ref.getId();
    }
    @Override public void saveActionItems(String uid, List<String> goals, Instant now) {
        if (goals.isEmpty()) return;
        WriteBatch batch = firestore.batch();
        for (String goal : goals) batch.set(actionItems(uid).document(), Map.of("text", goal, "completed", false, "createdAt", now.toEpochMilli()));
        wait(batch.commit());
    }
    @Override public List<JournalEntry> recentEntries(String uid, int maxResults) {
        try {
            return entries(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(maxResults).get().get().getDocuments().stream().map(d ->
                    new JournalEntry(d.getId(), d.getString("text"), d.getString("response"), Instant.ofEpochMilli(Objects.requireNonNullElse(d.getLong("createdAt"), 0L)), List.of())).toList();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); }
        catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); }
    }
    private List<JournalEntry> entriesWithEmbeddings(String uid, int maxResults) {
        try {
            return entries(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(maxResults).get().get().getDocuments().stream().map(d ->
                    new JournalEntry(d.getId(), d.getString("text"), d.getString("response"), Instant.ofEpochMilli(Objects.requireNonNullElse(d.getLong("createdAt"), 0L)), embedding(d.get("embedding")))).toList();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); }
        catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); }
    }
    @Override public List<JournalEntry> findRelevant(String uid, List<Double> queryEmbedding, int limit) {
        return entriesWithEmbeddings(uid, 100).stream().filter(entry -> !entry.embedding().isEmpty())
                .map(entry -> Map.entry(entry, cosine(queryEmbedding, entry.embedding()))).filter(entry -> entry.getValue() >= 0.55d)
                .sorted(Map.Entry.<JournalEntry, Double>comparingByValue().reversed()).limit(Math.max(1, Math.min(limit, 10))).map(Map.Entry::getKey).toList();
    }
    @Override public List<JournalEntry> listEntries(String uid) { return recentEntries(uid, 100); }
    @Override public List<ActionItem> listActionItems(String uid) {
        try {
            return actionItems(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get().get().getDocuments().stream().map(d ->
                    new ActionItem(d.getId(), d.getString("text"), Boolean.TRUE.equals(d.getBoolean("completed")), Instant.ofEpochMilli(Objects.requireNonNullElse(d.getLong("createdAt"), 0L)))).toList();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); }
        catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); }
    }
    @Override public void setActionItemCompleted(String uid, String id, boolean completed) { String valid = validId(id); wait(actionItems(uid).document(valid).update("completed", completed)); }
    @Override public void deleteActionItem(String uid, String id) { String valid = validId(id); wait(actionItems(uid).document(valid).delete()); }
    private String validId(String id) { if (id == null || !id.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException("Invalid document id"); return id; }
    private List<Double> embedding(Object value) { if (!(value instanceof List<?> values)) return List.of(); return values.stream().filter(Number.class::isInstance).map(Number.class::cast).map(Number::doubleValue).toList(); }
    private double cosine(List<Double> left, List<Double> right) {
        if (left.isEmpty() || left.size() != right.size()) return -1d;
        double dot = 0d, lm = 0d, rm = 0d; for (int i = 0; i < left.size(); i++) { dot += left.get(i) * right.get(i); lm += left.get(i) * left.get(i); rm += right.get(i) * right.get(i); }
        return lm == 0d || rm == 0d ? -1d : dot / (Math.sqrt(lm) * Math.sqrt(rm));
    }
    private void wait(com.google.api.core.ApiFuture<?> future) { try { future.get(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Firestore operation interrupted", e); } catch (ExecutionException e) { throw new IllegalStateException("Firestore operation failed", e.getCause()); } }
}
