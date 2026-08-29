package com.pm.personalgeminijournalbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
@EnableAsync
public class ApplicationConfig {
    @Bean
    FirebaseAuth firebaseAuth() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault()).build());
        }
        return FirebaseAuth.getInstance();
    }

    @Bean Firestore firestore() throws IOException {
        return FirestoreOptions.getDefaultInstance().getService();
    }
    @Bean SecretManagerServiceClient secretManagerServiceClient() throws IOException { return SecretManagerServiceClient.create(); }
    @Bean RestClient geminiRestClient(RestClient.Builder builder) { return builder.baseUrl("https://generativelanguage.googleapis.com").build(); }
    @Bean @ConfigurationProperties(prefix = "app.gemini") GeminiProperties geminiProperties() { return new GeminiProperties(); }
    @Bean @ConfigurationProperties(prefix = "app.cors") CorsProperties corsProperties() { return new CorsProperties(); }

    public static class GeminiProperties {
        private String apiKeySecret; private String model = "gemini-2.5-flash"; private String embeddingModel = "gemini-embedding-001";
        public String getApiKeySecret() { return apiKeySecret; } public void setApiKeySecret(String v) { apiKeySecret = v; }
        public String getModel() { return model; } public void setModel(String v) { model = v; }
        public String getEmbeddingModel() { return embeddingModel; } public void setEmbeddingModel(String v) { embeddingModel = v; }
    }
    public static class CorsProperties { private String allowedOrigins = ""; public String getAllowedOrigins() { return allowedOrigins; } public void setAllowedOrigins(String v) { allowedOrigins = v; } }
}
