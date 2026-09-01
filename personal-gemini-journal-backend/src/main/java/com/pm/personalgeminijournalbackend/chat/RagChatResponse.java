package com.pm.personalgeminijournalbackend.chat;
import java.util.List;
public record RagChatResponse(String reply, List<String> referencedEntries, String retrievalMode) {
    public RagChatResponse(String reply, List<String> referencedEntries) { this(reply, referencedEntries, "SEMANTIC_HYBRID"); }
}
