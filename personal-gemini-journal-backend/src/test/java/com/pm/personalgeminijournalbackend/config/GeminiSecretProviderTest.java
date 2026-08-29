package com.pm.personalgeminijournalbackend.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class GeminiSecretProviderTest {
    @Test void failsFastWhenSecretResourceIsBlank() {
        ApplicationConfig.GeminiProperties properties = new ApplicationConfig.GeminiProperties();
        GeminiSecretProvider provider = new GeminiSecretProvider(mock(SecretManagerServiceClient.class), properties);
        assertThrows(IllegalStateException.class, provider::apiKey);
    }

    @Test void readsSecretOnceThenCachesIt() {
        ApplicationConfig.GeminiProperties properties = new ApplicationConfig.GeminiProperties();
        properties.setApiKeySecret("projects/p/secrets/gemini/versions/latest");
        SecretManagerServiceClient client = mock(SecretManagerServiceClient.class);
        when(client.accessSecretVersion(argThat((AccessSecretVersionRequest request) -> request.getName().equals(properties.getApiKeySecret()))))
                .thenReturn(AccessSecretVersionResponse.newBuilder().setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("secret-value"))).build());
        GeminiSecretProvider provider = new GeminiSecretProvider(client, properties);

        assertEquals("secret-value", provider.apiKey());
        assertEquals("secret-value", provider.apiKey());
        verify(client, times(1)).accessSecretVersion(any(AccessSecretVersionRequest.class));
    }
}
