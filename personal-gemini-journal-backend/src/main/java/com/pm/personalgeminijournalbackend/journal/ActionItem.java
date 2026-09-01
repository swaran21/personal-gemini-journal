package com.pm.personalgeminijournalbackend.journal;
import java.time.Instant;
public record ActionItem(String id, String text, Status status, Instant createdAt) {
    public enum Status { PROPOSED, PENDING, COMPLETED }
}
