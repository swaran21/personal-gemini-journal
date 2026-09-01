package com.pm.personalgeminijournalbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

@Configuration
@Profile("cloud")
public class CloudApplicationConfig {
    @Bean FirebaseAuth firebaseAuth(ApplicationConfig.GoogleCloudProperties properties) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault());
            if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) options.setProjectId(properties.getProjectId());
            FirebaseApp.initializeApp(options.build());
        }
        return FirebaseAuth.getInstance();
    }

    @Bean Firestore firestore(ApplicationConfig.GoogleCloudProperties properties) throws IOException {
        FirestoreOptions.Builder options = FirestoreOptions.newBuilder().setCredentials(GoogleCredentials.getApplicationDefault());
        if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) options.setProjectId(properties.getProjectId());
        if (properties.getFirestoreDatabaseId() != null && !properties.getFirestoreDatabaseId().isBlank()) options.setDatabaseId(properties.getFirestoreDatabaseId());
        return options.build().getService();
    }

    @Bean SecretManagerServiceClient secretManagerServiceClient() throws IOException { return SecretManagerServiceClient.create(); }
    @Bean @Qualifier("geminiRestClient") RestClient geminiRestClient(RestClient.Builder builder) { return builder.baseUrl("https://generativelanguage.googleapis.com").build(); }
}
