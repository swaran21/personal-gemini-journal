package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.config.GeminiSecretProvider;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class GeminiService {
    private final RestClient client; private final GeminiSecretProvider secrets; private final ApplicationConfig.GeminiProperties properties; private final ObjectMapper mapper;
    public GeminiService(RestClient client, GeminiSecretProvider secrets, ApplicationConfig.GeminiProperties properties, ObjectMapper mapper) { this.client = client; this.secrets = secrets; this.properties = properties; this.mapper = mapper; }
    public GeminiResult reflect(String entry, List<JournalEntry> history) {
        String prompt = "You are a supportive personal journaling assistant. Give an empathetic, concise reply. Extract only explicit, actionable goals. Return JSON exactly matching the schema. Past entries (may be empty):\n" + history(history) + "\nCurrent entry:\n" + entry;
        Map<String, Object> schema = Map.of("type", "OBJECT", "properties", Map.of("reply", Map.of("type", "STRING"), "actionItems", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))), "required", List.of("reply", "actionItems"));
        JsonNode body = post(properties.getModel(), Map.of("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))), "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", schema)));
        try {
            JsonNode parsed = mapper.readTree(text(body)); String reply = parsed.path("reply").asText();
            if (reply.isBlank()) throw new IllegalStateException("Gemini returned an empty reply");
            List<String> goals = new ArrayList<>(); for (JsonNode n : parsed.path("actionItems")) { String goal = n.asText().trim(); if (!goal.isBlank() && goal.length() <= 1000) goals.add(goal); }
            return new GeminiResult(reply, goals.stream().distinct().limit(10).toList());
        } catch (Exception e) { throw new IllegalStateException("Gemini returned an invalid structured response", e); }
    }
    public List<Double> embed(String entry) {
        JsonNode body = post(properties.getEmbeddingModel(), Map.of("model", "models/" + properties.getEmbeddingModel(), "content", Map.of("parts", List.of(Map.of("text", entry)))));
        List<Double> values = new ArrayList<>(); for (JsonNode node : body.path("embedding").path("values")) values.add(node.asDouble());
        if (values.isEmpty()) throw new IllegalStateException("Gemini returned no embedding"); return values;
    }
    private JsonNode post(String model, Object request) { return client.post().uri(uri -> uri.path("/v1beta/models/{model}:{method}").queryParam("key", secrets.apiKey()).build(model, model.equals(properties.getEmbeddingModel()) ? "embedContent" : "generateContent")).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(JsonNode.class); }
    private String text(JsonNode response) { String value = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(); if (value.isBlank()) throw new IllegalStateException("Gemini returned no content"); return value; }
    private String history(List<JournalEntry> entries) { return entries.stream().map(e -> "User: " + e.text() + "\nAssistant: " + e.response()).reduce("", (a, b) -> a + "\n" + b); }
}
