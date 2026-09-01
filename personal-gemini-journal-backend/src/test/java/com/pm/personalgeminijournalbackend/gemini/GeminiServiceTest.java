package com.pm.personalgeminijournalbackend.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import com.pm.personalgeminijournalbackend.config.GeminiApiKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
}
