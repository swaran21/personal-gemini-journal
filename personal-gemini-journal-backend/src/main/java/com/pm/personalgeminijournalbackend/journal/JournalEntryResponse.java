package com.pm.personalgeminijournalbackend.journal;
import java.time.Instant;
/** Public journal-entry contract consumed by the React application. */
public record JournalEntryResponse(
        String id,
        String content,
        String aiResponse,
        String extractedGoal,
        Instant createdAt,
        JournalEntry.ProcessingStatus processingStatus,
        String processingError,
        GeoLocation location) {
    public JournalEntryResponse(String id, String content, String aiResponse, String extractedGoal, Instant createdAt, JournalEntry.ProcessingStatus processingStatus, String processingError) {
        this(id, content, aiResponse, extractedGoal, createdAt, processingStatus, processingError, null);
    }
}
