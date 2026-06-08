package com.sep490.hdbhms.notification.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FCMConfig {
    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount = getClass()
                    .getResourceAsStream("/filebase-service-account.json");
            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
