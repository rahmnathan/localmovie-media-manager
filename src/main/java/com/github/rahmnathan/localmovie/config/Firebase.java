package com.github.rahmnathan.localmovie.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

@Configuration
public class Firebase {

    @Bean
    @ConditionalOnProperty(name = "service.notificationsEnabled", havingValue = "true")
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnProperty(name = "service.notificationsEnabled", havingValue = "true")
    FirebaseApp firebaseApp() throws IOException {
        GoogleCredentials googleCredential = GoogleCredentials
                .fromStream(new FileInputStream("/workspace/secrets/google-services.json"))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(googleCredential)
                .build();

        return FirebaseApp.initializeApp(options);
    }
}
