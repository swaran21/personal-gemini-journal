package com.pm.personalgeminijournalbackend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalGeminiApiKeyProviderTest {
    @Test void trimsAndReturnsRuntimeKey() {
        assertEquals("runtime-key", new LocalGeminiApiKeyProvider(" runtime-key ").apiKey());
    }

    @Test void rejectsMissingKeyWithoutProvidingAPlaceholder() {
        assertThrows(IllegalStateException.class, new LocalGeminiApiKeyProvider(" ")::apiKey);
    }
}
