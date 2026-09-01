package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.gemini.EmbeddingService;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.journal.JournalEntryResponse;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class ChatService {
    private final GenerativeAiService gemini; private final EmbeddingService embeddings; private final JournalRepository journalRepository; private final AccountabilityDispatcher accountability;
    public ChatService(GenerativeAiService gemini, EmbeddingService embeddings, JournalRepository journalRepository, AccountabilityDispatcher accountability) { this.gemini = gemini; this.embeddings = embeddings; this.journalRepository = journalRepository; this.accountability = accountability; }
    public ChatResponse process(String uid, String rawEntry) {
        JournalEntryResponse result = processJournalEntry(uid, rawEntry);
        return new ChatResponse(result.aiResponse(), result.extractedGoal());
    }
    public JournalEntryResponse processJournalEntry(String uid, String rawEntry) {
        String entry = sanitize(rawEntry);
        Instant now = Instant.now();
        String id = journalRepository.createPendingEntry(uid, entry, now);
        accountability.dispatch(uid, id, entry, now);
        return new JournalEntryResponse(id, entry, null, null, now,
                com.pm.personalgeminijournalbackend.journal.JournalEntry.ProcessingStatus.PENDING, null);
    }
    public RagChatResponse chatWithPastSelf(String uid, String rawQuestion) {
        String question = sanitize(rawQuestion);
        var entries = journalRepository.findRelevant(uid, embeddings.embed(question), 5);
        String reply = gemini.answerWithGrounding(question, entries);
        var references = entries.stream().map(entry -> excerpt(entry.text())).toList();
        return new RagChatResponse(reply, references);
    }

    public JournalEntryResponse retryJournalEntry(String uid, String entryId) {
        var entry = journalRepository.retryEntryProcessing(uid, entryId, Instant.now());
        accountability.dispatch(uid, entry.id(), entry.text(), entry.createdAt());
        return new JournalEntryResponse(entry.id(), entry.text(), entry.response(), null, entry.createdAt(),
                com.pm.personalgeminijournalbackend.journal.JournalEntry.ProcessingStatus.PENDING, null);
    }

    private String excerpt(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 497) + "...";
    }

    private String sanitize(String value) {
        String result = value.replace("\u0000", "").trim();
        if (result.isBlank()) throw new IllegalArgumentException("Entry must contain non-whitespace text");
        return result;
    }
}
