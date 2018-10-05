package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.google.pushnotification.boundary.FirebaseNotificationService;
import com.github.rahmnathan.localmovie.domain.AndroidPushClient;
import com.github.rahmnathan.localmovie.media.manager.repository.AndroidPushTokenRepository;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PushNotificationHandlerTest {
    private static final String TOKEN_ID = "testTokenId";
    private static final String PUSH_TOKEN = "pushToken";
    private PushNotificationHandler notificationHandler;
    private AndroidPushTokenRepository tokenRepository;

    @BeforeEach
    public void initialize(){
        this.tokenRepository = mock(AndroidPushTokenRepository.class);

        FirebaseNotificationService notificationService = mock(FirebaseNotificationService.class);
        this.notificationHandler = new PushNotificationHandler(tokenRepository, mock(ProducerTemplate.class), mock(CamelContext.class));
    }

    @Test
    public void addMatchingTokenTest(){
        Optional<AndroidPushClient> pushClient = Optional.of(new AndroidPushClient(TOKEN_ID, PUSH_TOKEN));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(pushClient);

        notificationHandler.addPushToken(new AndroidPushClient(TOKEN_ID, PUSH_TOKEN));
    }

    @Test
    public void addNonMatchingTokenTest(){
        Optional<AndroidPushClient> pushClient = Optional.of(new AndroidPushClient(TOKEN_ID, "randomString"));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(pushClient);

        notificationHandler.addPushToken(new AndroidPushClient(TOKEN_ID, PUSH_TOKEN));
    }

    @Test
    public void addNewTokenTest(){
        Optional<AndroidPushClient> pushClient = Optional.empty();
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(pushClient);

        notificationHandler.addPushToken(new AndroidPushClient(TOKEN_ID, PUSH_TOKEN));
    }

    @Test
    public void sendNotificationsTest(){
        when(tokenRepository.findAll()).thenReturn(Collections.singleton(new AndroidPushClient(TOKEN_ID, PUSH_TOKEN)));

        notificationHandler.sendPushNotifications("FakeFilename", "FakeFilePath");
    }
}
