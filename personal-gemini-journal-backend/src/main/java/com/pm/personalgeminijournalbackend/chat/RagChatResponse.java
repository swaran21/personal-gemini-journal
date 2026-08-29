package com.pm.personalgeminijournalbackend.chat;
import java.time.Instant;
import java.util.List;
public record RagChatResponse(String reply, List<Reference> references) {
    public record Reference(String entryId, Instant createdAt, double similarity) { }
}
