package com.pm.personalgeminijournalbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Reads the development Gemini key from the process environment only. */
@Component
@Profile("gemini & !cloud")
public class LocalGeminiApiKeyProvider implements GeminiApiKeyProvider {
    private final String key;

    public LocalGeminiApiKeyProvider(@Value("${GEMINI_API_KEY:}") String key) {
        this.key = key == null ? "" : key.trim();
    }

    @Override
    public String apiKey() {
        if (key.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is required when the gemini profile is active");
        }
        return key;
    }
}
