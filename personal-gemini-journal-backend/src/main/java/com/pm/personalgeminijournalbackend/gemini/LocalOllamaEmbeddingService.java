package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Keeps embeddings local when Gemini is selected for higher-quality text generation. */
@Service
@Profile("local & gemini")
public class LocalOllamaEmbeddingService implements EmbeddingService {
    private final RestClient client;
    private final ApplicationConfig.OllamaProperties properties;

    public LocalOllamaEmbeddingService(
            @Qualifier("ollamaRestClient") RestClient client,
            ApplicationConfig.OllamaProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public List<Double> embed(String text) {
        JsonNode response = client.post().uri("/api/embed")
                .body(Map.of("model", properties.getEmbeddingModel(), "input", text))
                .retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("Local AI returned no embedding response");
        List<Double> values = new ArrayList<>();
        for (JsonNode value : response.path("embeddings").path(0)) values.add(value.asDouble());
        if (values.size() != properties.getEmbeddingDimensions()) {
            throw new IllegalStateException("Local AI returned an invalid embedding dimension");
        }
        return List.copyOf(values);
    }
}
