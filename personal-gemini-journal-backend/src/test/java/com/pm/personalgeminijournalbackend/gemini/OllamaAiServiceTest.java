package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaAiServiceTest {
    private MockRestServiceServer server;
    private OllamaAiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434");
        server = MockRestServiceServer.bindTo(builder).build();
        ApplicationConfig.OllamaProperties properties = new ApplicationConfig.OllamaProperties();
        properties.setEmbeddingDimensions(2);
        service = new OllamaAiService(builder.build(), properties, new ObjectMapper());
    }

    @Test
    void parsesStructuredReflectionWithoutLeakingPromptDataIntoConfiguration() {
        server.expect(once(), requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"message\":{\"content\":\"{\\\"reply\\\":\\\"You handled that thoughtfully.\\\"}\"}}", MediaType.APPLICATION_JSON));

        GeminiResult result = service.reflect("private entry", List.of());

        assertEquals("You handled that thoughtfully.", result.reply());
        server.verify();
    }

    @Test
    void rejectsAnEmbeddingWhoseDimensionDoesNotMatchPgvectorSchema() {
        server.expect(once(), requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[0.1]]}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> service.embed("entry"));
        server.verify();
    }

    @Test
    void returnsValidatedEmbeddingValues() {
        server.expect(once(), requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[0.1,0.2]]}", MediaType.APPLICATION_JSON));

        assertEquals(List.of(0.1d, 0.2d), service.embed("entry"));
        server.verify();
    }
}
