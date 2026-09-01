package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.config.GeminiApiKeyProvider;
import com.pm.personalgeminijournalbackend.chat.ChatTurn;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import com.pm.personalgeminijournalbackend.reflection.WeeklyReflection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

class GeminiServiceTest {
    private MockRestServiceServer server;
    private GeminiService service;

    @BeforeEach void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ApplicationConfig.GeminiProperties properties = new ApplicationConfig.GeminiProperties();
        properties.setModel("gemini-2.5-flash");
        GeminiApiKeyProvider keyProvider = () -> "runtime-key";
        service = new GeminiService(builder.build(), keyProvider, properties, new ObjectMapper());
    }

    @Test void sendsKeyInHeaderAndParsesStructuredReflection() {
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "runtime-key"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"reply\\\":\\\"You made meaningful progress today.\\\"}\"}]}}]}", MediaType.APPLICATION_JSON));

        assertEquals("You made meaningful progress today.", service.reflect("private entry", List.of()).reply());
        server.verify();
    }

    @Test void ragPromptIncludesTimestampsTemporalScopeAndBoundedConversation() {
        JournalEntry entry = new JournalEntry("id", "Built the RAG application", "You made progress",
                Instant.parse("2026-09-01T10:00:00Z"), List.of(), JournalEntry.ProcessingStatus.COMPLETED, null);
        RagContext context = new RagContext("What did I do today?", List.of(entry),
                List.of(new ChatTurn(ChatTurn.Role.USER, "What did I learn?"), new ChatTurn(ChatTurn.Role.ASSISTANT, "You learned RAG.")),
                Instant.parse("2026-09-01T12:00:00Z"), ZoneId.of("Asia/Kolkata"), "today");
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(content().string(allOf(containsString("Retrieval scope: today"), containsString("Built the RAG application"), containsString("What did I learn?"), containsString("2026-09-01T15:30+05:30"))))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"You built the RAG application.\"}]}}]}", MediaType.APPLICATION_JSON));

        assertEquals("You built the RAG application.", service.answerWithGrounding(context));
        server.verify();
    }

    @Test void weeklyReflectionRequiresEvidenceBasedSpecificOutput() {
        JournalEntry entry = new JournalEntry("id", "I built a RAG application", "Great progress",
                Instant.parse("2026-09-01T10:00:00Z"), List.of(), JournalEntry.ProcessingStatus.COMPLETED, null);
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(content().string(allOf(containsString("1-5 concrete highlights"), containsString("never a generic instruction"), containsString("I built a RAG application"))))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"highlights\\\":[\\\"You built a RAG application.\\\"],\\\"accomplishments\\\":[\\\"Completed a RAG build.\\\"],\\\"unresolvedThemes\\\":[],\\\"suggestedFocus\\\":\\\"Document the RAG architecture.\\\"}\"}]}}]}", MediaType.APPLICATION_JSON));

        WeeklyReflection result = service.generateWeeklyReflection(List.of(entry));

        assertEquals(List.of("You built a RAG application."), result.highlights());
        assertEquals("Document the RAG architecture.", result.suggestedFocus());
        server.verify();
    }

    @Test void refusesNonFlashGenerationModels() {
        ApplicationConfig.GeminiProperties properties = new ApplicationConfig.GeminiProperties();
        properties.setModel("gemini-2.5-pro");
        GeminiService nonFlash = new GeminiService(RestClient.builder().baseUrl("https://generativelanguage.googleapis.com").build(), () -> "runtime-key", properties, new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> nonFlash.reflect("private entry", List.of()));
    }
}
