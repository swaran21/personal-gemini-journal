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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
@EnableAsync
public class ApplicationConfig {
    @Bean
    FirebaseAuth firebaseAuth(GoogleCloudProperties cloudProperties) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault());
            if (cloudProperties.getProjectId() != null && !cloudProperties.getProjectId().isBlank()) options.setProjectId(cloudProperties.getProjectId());
            FirebaseApp.initializeApp(options.build());
        }
        return FirebaseAuth.getInstance();
    }

    @Bean Firestore firestore(GoogleCloudProperties cloudProperties) throws IOException {
        FirestoreOptions.Builder options = FirestoreOptions.newBuilder().setCredentials(GoogleCredentials.getApplicationDefault());
        if (cloudProperties.getProjectId() != null && !cloudProperties.getProjectId().isBlank()) options.setProjectId(cloudProperties.getProjectId());
        if (cloudProperties.getFirestoreDatabaseId() != null && !cloudProperties.getFirestoreDatabaseId().isBlank()) options.setDatabaseId(cloudProperties.getFirestoreDatabaseId());
        return options.build().getService();
    }
    @Bean SecretManagerServiceClient secretManagerServiceClient() throws IOException { return SecretManagerServiceClient.create(); }
    @Bean RestClient geminiRestClient(RestClient.Builder builder) { return builder.baseUrl("https://generativelanguage.googleapis.com").build(); }
    @Bean @ConfigurationProperties(prefix = "app.gemini") GeminiProperties geminiProperties() { return new GeminiProperties(); }
    @Bean @ConfigurationProperties(prefix = "app.cors") CorsProperties corsProperties() { return new CorsProperties(); }
    @Bean @ConfigurationProperties(prefix = "app.google-cloud") GoogleCloudProperties googleCloudProperties() { return new GoogleCloudProperties(); }
    @Bean(name = "accountabilityExecutor")
    TaskExecutor accountabilityExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(4); executor.setQueueCapacity(100); executor.setThreadNamePrefix("accountability-"); executor.initialize();
        return executor;
    }

    public static class GeminiProperties {
        private String apiKeySecret; private String model = "gemini-2.5-flash"; private String embeddingModel = "gemini-embedding-001";
        public String getApiKeySecret() { return apiKeySecret; } public void setApiKeySecret(String v) { apiKeySecret = v; }
        public String getModel() { return model; } public void setModel(String v) { model = v; }
        public String getEmbeddingModel() { return embeddingModel; } public void setEmbeddingModel(String v) { embeddingModel = v; }
    }
    public static class CorsProperties { private String allowedOrigins = ""; public String getAllowedOrigins() { return allowedOrigins; } public void setAllowedOrigins(String v) { allowedOrigins = v; } }
    public static class GoogleCloudProperties {
        private String projectId; private String firestoreDatabaseId;
        public String getProjectId() { return projectId; } public void setProjectId(String v) { projectId = v; }
        public String getFirestoreDatabaseId() { return firestoreDatabaseId; } public void setFirestoreDatabaseId(String v) { firestoreDatabaseId = v; }
    }
}
