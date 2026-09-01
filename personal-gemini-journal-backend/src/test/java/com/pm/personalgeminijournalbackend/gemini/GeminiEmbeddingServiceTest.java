package com.pm.personalgeminijournalbackend.gemini;

import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class GeminiEmbeddingServiceTest {
    private GeminiEmbeddingService service;
    private MockRestServiceServer server;

    @BeforeEach void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ApplicationConfig.GeminiProperties properties = new ApplicationConfig.GeminiProperties();
        properties.setEmbeddingDimensions(768);
        service = new GeminiEmbeddingService(builder.build(), () -> "runtime-key", properties);
    }

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

    @Test void requestsConfigured768DimensionsAndNormalizesGeminiEmbedding() {
        String vector = "[" + "1,".repeat(767) + "1]";
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent"))
                .andExpect(header("x-goog-api-key", "runtime-key"))
                .andExpect(content().string(containsString("\"outputDimensionality\":768")))
                .andRespond(withSuccess("{\"embedding\":{\"values\":" + vector + "}}", MediaType.APPLICATION_JSON));

        List<Double> embedding = service.embed("private journal text");

        assertEquals(768, embedding.size());
        assertEquals(1d, Math.sqrt(embedding.stream().mapToDouble(value -> value * value).sum()), 0.000001d);
        server.verify();
    }

    private JournalEntry entry(String id, List<Double> embedding) {
        return new JournalEntry(id, "text", "response", Instant.EPOCH, embedding, JournalEntry.ProcessingStatus.COMPLETED, null);
    }
}
