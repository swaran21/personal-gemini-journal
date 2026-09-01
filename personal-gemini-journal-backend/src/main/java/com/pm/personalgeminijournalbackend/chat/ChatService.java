package com.pm.personalgeminijournalbackend.chat;

import com.pm.personalgeminijournalbackend.gemini.GenerativeAiService;
import com.pm.personalgeminijournalbackend.gemini.EmbeddingService;
import com.pm.personalgeminijournalbackend.gemini.RagContext;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.journal.JournalEntryResponse;
import com.pm.personalgeminijournalbackend.journal.GeoLocation;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatService {
    private static final DateTimeFormatter REFERENCE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
    private final GenerativeAiService gemini; private final EmbeddingService embeddings; private final JournalRepository journalRepository; private final AccountabilityDispatcher accountability; private final TemporalQueryResolver temporalQueries;
    public ChatService(GenerativeAiService gemini, EmbeddingService embeddings, JournalRepository journalRepository, AccountabilityDispatcher accountability, TemporalQueryResolver temporalQueries) { this.gemini = gemini; this.embeddings = embeddings; this.journalRepository = journalRepository; this.accountability = accountability; this.temporalQueries = temporalQueries; }
    public ChatResponse process(String uid, String rawEntry) {
        JournalEntryResponse result = processJournalEntry(uid, rawEntry);
        return new ChatResponse(result.aiResponse(), result.extractedGoal());
    }
    public JournalEntryResponse processJournalEntry(String uid, String rawEntry) {
        return processJournalEntry(uid, rawEntry, null);
    }
    public JournalEntryResponse processJournalEntry(String uid, String rawEntry, GeoLocation location) {
        String entry = sanitize(rawEntry);
        Instant now = Instant.now();
        String id = journalRepository.createPendingEntry(uid, entry, location, now);
        accountability.dispatch(uid, id, entry, now);
        return new JournalEntryResponse(id, entry, null, null, now,
                com.pm.personalgeminijournalbackend.journal.JournalEntry.ProcessingStatus.PENDING, null, location);
    }
    public RagChatResponse chatWithPastSelf(String uid, String rawQuestion) {
        return chatWithPastSelf(uid, new RagChatRequest(rawQuestion));
    }

    public RagChatResponse chatWithPastSelf(String uid, RagChatRequest request) {
        String question = sanitize(request.question());
        ZoneId zone = parseZone(request.timeZone());
        var timeRange = temporalQueries.resolve(question, zone);
        var entries = timeRange
                .map(range -> journalRepository.entriesBetween(uid, range.startInclusive(), range.endExclusive(), 100))
                .orElseGet(() -> journalRepository.findRelevant(uid, embeddings.embed(question), 5));
        List<ChatTurn> conversation = request.history().stream()
                .limit(10)
                .map(message -> new ChatTurn(message.role() == RagChatRequest.Role.USER ? ChatTurn.Role.USER : ChatTurn.Role.ASSISTANT, sanitize(message.content())))
                .toList();
        String reply = gemini.answerWithGrounding(new RagContext(question, entries, conversation,
                temporalQueries.now(), zone, timeRange.map(TemporalQueryResolver.TimeRange::label).orElse(null)));
        var references = entries.stream().map(entry -> "[" + REFERENCE_TIME.withZone(zone).format(entry.createdAt()) + "] " + excerpt(entry.text())).toList();
        return new RagChatResponse(reply, references);
    }

    public JournalEntryResponse retryJournalEntry(String uid, String entryId) {
        var entry = journalRepository.retryEntryProcessing(uid, entryId, Instant.now());
        accountability.dispatch(uid, entry.id(), entry.text(), entry.createdAt());
        return new JournalEntryResponse(entry.id(), entry.text(), entry.response(), null, entry.createdAt(),
                com.pm.personalgeminijournalbackend.journal.JournalEntry.ProcessingStatus.PENDING, null, entry.location());
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

    private ZoneId parseZone(String value) {
        if (value == null || value.isBlank()) return ZoneId.of("UTC");
        try { return ZoneId.of(value.trim()); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("timeZone must be a valid IANA time zone"); }
    }
}
