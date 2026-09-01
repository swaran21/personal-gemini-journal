package com.pm.personalgeminijournalbackend.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/** Obtains the Gemini credential from Google Secret Manager once per instance; never logs it. */
@Component
@Profile("cloud")
public class GeminiSecretProvider implements GeminiApiKeyProvider {
    private final SecretManagerServiceClient client; private final ApplicationConfig.GeminiProperties properties; private volatile String key;
    public GeminiSecretProvider(SecretManagerServiceClient client, ApplicationConfig.GeminiProperties properties) { this.client = client; this.properties = properties; }
    @Override public String apiKey() {
        String result = key;
        if (result != null) return result;
        synchronized (this) {
            if (key == null) {
                if (properties.getApiKeySecret() == null || properties.getApiKeySecret().isBlank()) throw new IllegalStateException("GEMINI_API_KEY_SECRET must name a Secret Manager version");
                key = client.accessSecretVersion(AccessSecretVersionRequest.newBuilder().setName(properties.getApiKeySecret()).build()).getPayload().getData().toStringUtf8();
            }
            return key;
        }
    }
}
