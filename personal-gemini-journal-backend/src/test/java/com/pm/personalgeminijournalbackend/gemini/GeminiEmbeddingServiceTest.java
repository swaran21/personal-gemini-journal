package com.pm.personalgeminijournalbackend.gemini;

import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.config.GeminiSecretProvider;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GeminiEmbeddingServiceTest {
    private final GeminiEmbeddingService service = new GeminiEmbeddingService(mock(RestClient.class), mock(GeminiSecretProvider.class), new ApplicationConfig.GeminiProperties());

    @Test void cosineSimilarityReturnsExpectedScoreForParallelVectors() {
        assertEquals(1d, service.cosineSimilarity(List.of(3d, 4d), List.of(6d, 8d)), 0.00001d);
    }

    @Test void cosineSimilarityRejectsEmptyMismatchedAndZeroVectors() {
        assertEquals(-1d, service.cosineSimilarity(List.of(), List.of()));
        assertEquals(-1d, service.cosineSimilarity(List.of(1d), List.of(1d, 2d)));
        assertEquals(-1d, service.cosineSimilarity(List.of(0d, 0d), List.of(1d, 0d)));
    }

    @Test void mostRelevantFiltersWeakAndMissingEmbeddingsThenSortsDescending() {
        JournalEntry strongest = entry("strongest", List.of(1d, 0d));
        JournalEntry weak = entry("weak", List.of(0.5d, 0.866d));
        JournalEntry missing = entry("missing", List.of());

        List<GeminiEmbeddingService.RetrievedEntry> results = service.mostRelevant(List.of(1d, 0d), List.of(weak, missing, strongest), 5);

        assertEquals(1, results.size());
        assertEquals("strongest", results.get(0).entry().id());
    }

    @Test void mostRelevantClampsRequestedLimitAndCandidateCount() {
        List<JournalEntry> candidates = IntStream.range(0, 120).mapToObj(index -> entry("entry-" + index, List.of(1d, 0d))).toList();

        List<GeminiEmbeddingService.RetrievedEntry> results = service.mostRelevant(List.of(1d, 0d), candidates, 100);

        assertEquals(10, results.size());
        assertEquals("entry-0", results.get(0).entry().id());
    }

    @Test void mostRelevantUsesAtLeastOneResultWhenLimitIsZero() {
        List<GeminiEmbeddingService.RetrievedEntry> results = service.mostRelevant(List.of(1d, 0d), List.of(entry("one", List.of(1d, 0d))), 0);
        assertEquals(1, results.size());
    }

    private JournalEntry entry(String id, List<Double> embedding) {
        return new JournalEntry(id, "text", "response", Instant.EPOCH, embedding);
    }
}
