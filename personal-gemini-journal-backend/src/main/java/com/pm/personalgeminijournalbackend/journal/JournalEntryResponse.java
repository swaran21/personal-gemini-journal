package com.pm.personalgeminijournalbackend.journal;
import java.time.Instant;
/** Public journal-entry contract consumed by the React application. */
public record JournalEntryResponse(String id, String content, String aiResponse, String extractedGoal, Instant createdAt) { }
