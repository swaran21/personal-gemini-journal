package com.pm.personalgeminijournalbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class ApplicationConfig {
    @Bean @ConfigurationProperties(prefix = "app.gemini") GeminiProperties geminiProperties() { return new GeminiProperties(); }
    @Bean @ConfigurationProperties(prefix = "app.ollama") OllamaProperties ollamaProperties() { return new OllamaProperties(); }
    @Bean @ConfigurationProperties(prefix = "app.cors") CorsProperties corsProperties() { return new CorsProperties(); }
    @Bean @ConfigurationProperties(prefix = "app.google-cloud") GoogleCloudProperties googleCloudProperties() { return new GoogleCloudProperties(); }
    @Bean(name = "accountabilityExecutor")
    TaskExecutor accountabilityExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(4); executor.setQueueCapacity(100); executor.setThreadNamePrefix("accountability-"); executor.initialize();
        return executor;
    }

    public static class GeminiProperties {
        private String apiKeySecret; private String model = "gemini-3.6-flash"; private String embeddingModel = "gemini-embedding-001"; private int embeddingDimensions = 768;
        public String getApiKeySecret() { return apiKeySecret; } public void setApiKeySecret(String v) { apiKeySecret = v; }
        public String getModel() { return model; } public void setModel(String v) { model = v; }
        public String getEmbeddingModel() { return embeddingModel; } public void setEmbeddingModel(String v) { embeddingModel = v; }
        public int getEmbeddingDimensions() { return embeddingDimensions; } public void setEmbeddingDimensions(int v) { embeddingDimensions = v; }
    }
    public static class OllamaProperties {
        private String baseUrl = "http://localhost:11434"; private String chatModel = "gemma3:1b"; private String embeddingModel = "nomic-embed-text"; private int embeddingDimensions = 768;
        public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String v) { baseUrl = v; }
        public String getChatModel() { return chatModel; } public void setChatModel(String v) { chatModel = v; }
        public String getEmbeddingModel() { return embeddingModel; } public void setEmbeddingModel(String v) { embeddingModel = v; }
        public int getEmbeddingDimensions() { return embeddingDimensions; } public void setEmbeddingDimensions(int v) { embeddingDimensions = v; }
    }
    public static class CorsProperties { private String allowedOrigins = ""; public String getAllowedOrigins() { return allowedOrigins; } public void setAllowedOrigins(String v) { allowedOrigins = v; } }
    public static class GoogleCloudProperties {
        private String projectId; private String firestoreDatabaseId;
        public String getProjectId() { return projectId; } public void setProjectId(String v) { projectId = v; }
        public String getFirestoreDatabaseId() { return firestoreDatabaseId; } public void setFirestoreDatabaseId(String v) { firestoreDatabaseId = v; }
    }
}
