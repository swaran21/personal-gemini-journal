package com.pm.personalgeminijournalbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("local")
public class LocalAiConfig {
    @Bean @Qualifier("ollamaRestClient") RestClient ollamaRestClient(RestClient.Builder builder, ApplicationConfig.OllamaProperties properties) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
    @Bean @Profile("gemini") @Qualifier("geminiRestClient") RestClient localGeminiRestClient(RestClient.Builder builder) {
        return builder.baseUrl("https://generativelanguage.googleapis.com").build();
    }
}
