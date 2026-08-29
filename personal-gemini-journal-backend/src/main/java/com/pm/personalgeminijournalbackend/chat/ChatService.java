package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GeminiResult;
import com.pm.personalgeminijournalbackend.gemini.GeminiService;
import com.pm.personalgeminijournalbackend.gemini.GeminiEmbeddingService;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class ChatService {
    private final GeminiService gemini; private final GeminiEmbeddingService embeddings; private final JournalRepository journalRepository; private final AccountabilityService accountability;
    public ChatService(GeminiService gemini, GeminiEmbeddingService embeddings, JournalRepository journalRepository, AccountabilityService accountability) { this.gemini = gemini; this.embeddings = embeddings; this.journalRepository = journalRepository; this.accountability = accountability; }
    public ChatResponse process(String uid, String rawEntry) {
        String entry = sanitize(rawEntry);
        GeminiResult result = gemini.reflect(entry, journalRepository.recentEntries(uid, 10));
        Instant now = Instant.now();
        journalRepository.saveEntry(uid, entry, result.reply(), embeddings.embed(entry), now);
        accountability.extractAndPersist(uid, entry, now);
        return new ChatResponse(result.reply(), null);
    }
    public RagChatResponse chatWithPastSelf(String uid, String rawQuestion) {
        String question = sanitize(rawQuestion);
        var matches = embeddings.mostRelevant(embeddings.embed(question), journalRepository.entriesWithEmbeddings(uid, 100), 5);
        var entries = matches.stream().map(GeminiEmbeddingService.RetrievedEntry::entry).toList();
        String reply = gemini.answerWithGrounding(question, entries);
        var references = matches.stream().map(match -> new RagChatResponse.Reference(match.entry().id(), match.entry().createdAt(), match.score())).toList();
        return new RagChatResponse(reply, references);
    }
    private String sanitize(String value) {
        String result = value.replace("\u0000", "").trim();
        if (result.isBlank()) throw new IllegalArgumentException("Entry must contain non-whitespace text");
        return result;
    }
}
