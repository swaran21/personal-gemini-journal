package com.pm.personalgeminijournalbackend.journal;
import java.time.Instant;
import java.util.List;
public record JournalEntry(
        String id,
        String text,
        String response,
        Instant createdAt,
        List<Double> embedding,
        ProcessingStatus processingStatus,
        String processingError) {
    public enum ProcessingStatus { PENDING, COMPLETED, FAILED }
}
