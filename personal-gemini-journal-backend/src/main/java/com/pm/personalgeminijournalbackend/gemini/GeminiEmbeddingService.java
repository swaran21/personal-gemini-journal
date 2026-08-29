package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.config.GeminiSecretProvider;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Generates Gemini embeddings and performs bounded in-memory similarity ranking for one user's entries. */
@Service
public class GeminiEmbeddingService {
    private static final int MAX_CANDIDATES = 100;
    private final RestClient client;
    private final GeminiSecretProvider secrets;
    private final ApplicationConfig.GeminiProperties properties;

    public GeminiEmbeddingService(RestClient client, GeminiSecretProvider secrets, ApplicationConfig.GeminiProperties properties) {
        this.client = client; this.secrets = secrets; this.properties = properties;
    }

    public List<Double> embed(String text) {
        JsonNode response = client.post().uri(uri -> uri.path("/v1beta/models/{model}:embedContent")
                        .queryParam("key", secrets.apiKey()).build(properties.getEmbeddingModel()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", "models/" + properties.getEmbeddingModel(), "content", Map.of("parts", List.of(Map.of("text", text)))))
                .retrieve().body(JsonNode.class);
        List<Double> values = new ArrayList<>();
        for (JsonNode node : response.path("embedding").path("values")) values.add(node.asDouble());
        if (values.isEmpty()) throw new IllegalStateException("Gemini returned no embedding");
        return List.copyOf(values);
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

    public record RetrievedEntry(JournalEntry entry, double score) { }
}
