package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GeminiResult;
import com.pm.personalgeminijournalbackend.gemini.GeminiService;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class ChatService {
    private final GeminiService gemini; private final JournalRepository journalRepository; private final AccountabilityService accountability;
    public ChatService(GeminiService gemini, JournalRepository journalRepository, AccountabilityService accountability) { this.gemini = gemini; this.journalRepository = journalRepository; this.accountability = accountability; }
    public ChatResponse process(String uid, String rawEntry) {
        String entry = sanitize(rawEntry);
        GeminiResult result = gemini.reflect(entry, journalRepository.recentEntries(uid, 10));
        Instant now = Instant.now();
        journalRepository.saveEntry(uid, entry, result.reply(), gemini.embed(entry), now);
        accountability.persist(uid, result.actionItems(), now);
        return new ChatResponse(result.reply(), result.actionItems().isEmpty() ? null : result.actionItems().get(0));
    }
    private String sanitize(String value) {
        String result = value.replace("\u0000", "").trim();
        if (result.isBlank()) throw new IllegalArgumentException("Entry must contain non-whitespace text");
        return result;
    }
}
