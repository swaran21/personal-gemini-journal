package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.config.GeminiApiKeyProvider;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;

/** Generates Gemini embeddings and performs bounded in-memory similarity ranking for one user's entries. */
@Service
@Profile({"cloud", "gemini"})
public class GeminiEmbeddingService implements EmbeddingService {
    private static final int MAX_CANDIDATES = 100;
    private final RestClient client;
    private final GeminiApiKeyProvider secrets;
    private final ApplicationConfig.GeminiProperties properties;

    public GeminiEmbeddingService(@Qualifier("geminiRestClient") RestClient client, GeminiApiKeyProvider secrets, ApplicationConfig.GeminiProperties properties) {
        this.client = client; this.secrets = secrets; this.properties = properties;
    }

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text must not be blank");
        int dimensions = properties.getEmbeddingDimensions();
        if (dimensions < 128 || dimensions > 3072) throw new IllegalArgumentException("Gemini embedding dimensions must be between 128 and 3072");
        JsonNode response = client.post().uri("/v1beta/models/{model}:embedContent", properties.getEmbeddingModel())
                .header("x-goog-api-key", secrets.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", "models/" + properties.getEmbeddingModel(), "content", Map.of("parts", List.of(Map.of("text", text))), "outputDimensionality", dimensions))
                .retrieve().body(JsonNode.class);
        List<Double> values = new ArrayList<>();
        if (response == null) throw new IllegalStateException("Gemini returned no embedding response");
        for (JsonNode node : response.path("embedding").path("values")) values.add(node.asDouble());
        if (values.size() != dimensions) throw new IllegalStateException("Gemini returned an invalid embedding dimension");
        return normalize(values);
    }

    public List<RetrievedEntry> mostRelevant(List<Double> queryEmbedding, List<JournalEntry> candidates, int limit) {
        return candidates.stream().limit(MAX_CANDIDATES)
                .filter(entry -> !entry.embedding().isEmpty())
                .map(entry -> new RetrievedEntry(entry, cosineSimilarity(queryEmbedding, entry.embedding())))
                .filter(result -> result.score() >= 0.55d)
                .sorted(Comparator.comparingDouble(RetrievedEntry::score).reversed())
                .limit(Math.max(1, Math.min(limit, 10))).toList();
    }

    double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left.size() != right.size() || left.isEmpty()) return -1d;
        double dot = 0d, leftMagnitude = 0d, rightMagnitude = 0d;
        for (int index = 0; index < left.size(); index++) {
            double l = left.get(index), r = right.get(index);
            dot += l * r; leftMagnitude += l * l; rightMagnitude += r * r;
        }
        if (leftMagnitude == 0d || rightMagnitude == 0d) return -1d;
        return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }

    private List<Double> normalize(List<Double> values) {
        double magnitude = 0d;
        for (Double value : values) magnitude += value * value;
        if (magnitude == 0d) throw new IllegalStateException("Gemini returned a zero embedding");
        double divisor = Math.sqrt(magnitude);
        return values.stream().map(value -> value / divisor).toList();
    }

    public record RetrievedEntry(JournalEntry entry, double score) { }
}
