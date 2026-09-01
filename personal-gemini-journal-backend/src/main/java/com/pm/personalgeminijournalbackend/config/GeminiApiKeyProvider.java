package com.pm.personalgeminijournalbackend.config;

/** Server-side credential port. Implementations must never log or expose the returned key. */
public interface GeminiApiKeyProvider {
    String apiKey();
}
