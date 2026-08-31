package com.pm.personalgeminijournalbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("local")
public class LocalAiConfig {
    @Bean RestClient ollamaRestClient(RestClient.Builder builder, ApplicationConfig.OllamaProperties properties) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
