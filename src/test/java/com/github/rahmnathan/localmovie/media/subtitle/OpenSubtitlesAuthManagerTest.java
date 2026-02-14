package com.github.rahmnathan.localmovie.media.subtitle;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.OpenSubtitlesApi;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.OpenSubtitlesAuthManager;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesLoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenSubtitlesAuthManagerTest {

    @Mock
    private ServiceConfig serviceConfig;
    @Mock
    private ServiceConfig.OpenSubtitlesConfig openSubtitlesConfig;
    @Mock
    private OpenSubtitlesApi openSubtitlesApi;

    private OpenSubtitlesAuthManager authManager;

    @BeforeEach
    void setUp() {
        when(serviceConfig.getOpensubtitles()).thenReturn(openSubtitlesConfig);
        authManager = new OpenSubtitlesAuthManager(serviceConfig, openSubtitlesApi);
    }

    @Test
    void getToken_authenticatesOnFirstCall() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");

        OpenSubtitlesLoginResponse loginResponse = new OpenSubtitlesLoginResponse();
        loginResponse.setToken("test-token-123");
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(loginResponse);

        String token = authManager.getToken();

        assertEquals("test-token-123", token);
        verify(openSubtitlesApi).login(eq("testapikey"), any());
    }

    @Test
    void getToken_cachesToken() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");

        OpenSubtitlesLoginResponse loginResponse = new OpenSubtitlesLoginResponse();
        loginResponse.setToken("cached-token");
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(loginResponse);

        // First call
        String token1 = authManager.getToken();
        // Second call - should use cache
        String token2 = authManager.getToken();

        assertEquals(token1, token2);
        // API should only be called once
        verify(openSubtitlesApi, times(1)).login(anyString(), any());
    }

    @Test
    void getToken_setsRemainingDownloadsFromLogin() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");

        OpenSubtitlesLoginResponse.UserInfo userInfo = new OpenSubtitlesLoginResponse.UserInfo();
        userInfo.setAllowedDownloads(100);
        userInfo.setDownloadsCount(25);

        OpenSubtitlesLoginResponse loginResponse = new OpenSubtitlesLoginResponse();
        loginResponse.setToken("test-token");
        loginResponse.setUser(userInfo);
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(loginResponse);

        authManager.getToken();

        assertEquals(75, authManager.getRemainingDownloads());
    }

    @Test
    void getToken_returnsNullOnLoginFailure() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(null);

        String token = authManager.getToken();

        assertNull(token);
    }

    @Test
    void getToken_returnsNullWhenNoTokenInResponse() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");

        OpenSubtitlesLoginResponse loginResponse = new OpenSubtitlesLoginResponse();
        loginResponse.setToken(null);
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(loginResponse);

        String token = authManager.getToken();

        assertNull(token);
    }

    @Test
    void getToken_handlesExceptionGracefully() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");
        when(openSubtitlesApi.login(anyString(), any())).thenThrow(new RuntimeException("Network error"));

        String token = authManager.getToken();

        assertNull(token);
    }

    @Test
    void hasDownloadsRemaining_returnsTrueByDefault() {
        // Default is -1 (unknown), which should return true
        assertTrue(authManager.hasDownloadsRemaining());
    }

    @Test
    void hasDownloadsRemaining_returnsFalseWhenZero() {
        authManager.updateRemainingDownloads(0);
        assertFalse(authManager.hasDownloadsRemaining());
    }

    @Test
    void hasDownloadsRemaining_returnsTrueWhenPositive() {
        authManager.updateRemainingDownloads(50);
        assertTrue(authManager.hasDownloadsRemaining());
    }

    @Test
    void updateRemainingDownloads_updatesValue() {
        authManager.updateRemainingDownloads(42);
        assertEquals(42, authManager.getRemainingDownloads());

        authManager.updateRemainingDownloads(10);
        assertEquals(10, authManager.getRemainingDownloads());
    }

    @Test
    void refreshQuota_clearsTokenAndReauthenticates() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");

        OpenSubtitlesLoginResponse.UserInfo userInfo = new OpenSubtitlesLoginResponse.UserInfo();
        userInfo.setAllowedDownloads(100);
        userInfo.setDownloadsCount(0);

        OpenSubtitlesLoginResponse loginResponse = new OpenSubtitlesLoginResponse();
        loginResponse.setToken("new-token");
        loginResponse.setUser(userInfo);
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(loginResponse);

        // Set some existing values
        authManager.updateRemainingDownloads(5);

        // Refresh quota
        authManager.refreshQuota();

        // Should have reauthenticated and updated quota
        assertEquals(100, authManager.getRemainingDownloads());
        verify(openSubtitlesApi).login(anyString(), any());
    }

    @Test
    void refreshQuota_resetsRemainingToUnknown() {
        when(openSubtitlesConfig.getUsername()).thenReturn("testuser");
        when(openSubtitlesConfig.getPassword()).thenReturn("testpass");
        when(openSubtitlesConfig.getApiKey()).thenReturn("testapikey");
        when(openSubtitlesApi.login(anyString(), any())).thenReturn(null);

        authManager.updateRemainingDownloads(5);
        authManager.refreshQuota();

        // Should reset to -1 even if login fails
        assertEquals(-1, authManager.getRemainingDownloads());
    }
}
