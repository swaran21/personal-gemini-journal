package com.pm.personalgeminijournalbackend.gemini;

import com.pm.personalgeminijournalbackend.chat.ChatTurn;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public record RagContext(String question, List<JournalEntry> entries, List<ChatTurn> conversation,
                         Instant currentTime, ZoneId timeZone, String temporalScope) {
    public RagContext {
        entries = List.copyOf(entries);
        conversation = List.copyOf(conversation);
    }
}
