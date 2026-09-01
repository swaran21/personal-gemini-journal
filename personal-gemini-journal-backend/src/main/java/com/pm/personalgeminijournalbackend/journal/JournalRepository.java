package com.pm.personalgeminijournalbackend.journal;

import java.time.Instant;
import java.util.List;

/** Persistence port. Every implementation must scope every operation to the supplied authenticated UID. */
public interface JournalRepository {
    /** Persists the user's text before any AI provider is invoked. */
    String createPendingEntry(String uid, String text, GeoLocation location, Instant now);
    default String createPendingEntry(String uid, String text, Instant now) { return createPendingEntry(uid, text, null, now); }
    void completeEntryProcessing(String uid, String entryId, String reply, List<Double> embedding);
    void failEntryProcessing(String uid, String entryId, String safeError);
    JournalEntry retryEntryProcessing(String uid, String entryId, Instant now);
    void saveActionItems(String uid, List<String> goals, Instant now);
    default void saveActionItems(String uid, String sourceEntryId, List<String> goals, Instant now) {
        saveActionItems(uid, goals, now);
    }
    List<JournalEntry> recentEntries(String uid, int maxResults);
    List<JournalEntry> entriesBetween(String uid, Instant startInclusive, Instant endExclusive, int maxResults);
    PageSlice<JournalEntry> listEntries(String uid, int limit, String cursor);
    PageSlice<ActionItem> listActionItems(String uid, int limit, String cursor);
    default List<JournalEntry> listEntries(String uid) { return listEntries(uid, 100, null).items(); }
    default List<ActionItem> listActionItems(String uid) { return listActionItems(uid, 100, null).items(); }
    List<JournalEntry> findRelevant(String uid, List<Double> queryEmbedding, int limit);
    void setActionItemStatus(String uid, String id, ActionItem.Status status);
    void deleteActionItem(String uid, String id);
    /** Permanently deletes every application record owned by the authenticated UID. */
    void deleteAllUserData(String uid);
}
