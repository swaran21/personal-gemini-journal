package com.pm.personalgeminijournalbackend.journal;

import java.time.Instant;
import java.util.List;

/** Persistence port. Every implementation must scope every operation to the supplied authenticated UID. */
public interface JournalRepository {
    /** Persists the user's text before any AI provider is invoked. */
    String createPendingEntry(String uid, String text, Instant now);
    void completeEntryProcessing(String uid, String entryId, String reply, List<Double> embedding);
    void failEntryProcessing(String uid, String entryId, String safeError);
    void saveActionItems(String uid, List<String> goals, Instant now);
    default void saveActionItems(String uid, String sourceEntryId, List<String> goals, Instant now) {
        saveActionItems(uid, goals, now);
    }
    List<JournalEntry> recentEntries(String uid, int maxResults);
    List<JournalEntry> listEntries(String uid);
    List<ActionItem> listActionItems(String uid);
    List<JournalEntry> findRelevant(String uid, List<Double> queryEmbedding, int limit);
    void setActionItemStatus(String uid, String id, ActionItem.Status status);
    void deleteActionItem(String uid, String id);
    /** Permanently deletes every application record owned by the authenticated UID. */
    void deleteAllUserData(String uid);
}
