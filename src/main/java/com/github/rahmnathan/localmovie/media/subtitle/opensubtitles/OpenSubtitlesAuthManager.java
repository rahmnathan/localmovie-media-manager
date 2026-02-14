package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesLoginRequest;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class OpenSubtitlesAuthManager {
    private static final long TOKEN_VALIDITY_SECONDS = 3600 * 23; // 23 hours

    private final ServiceConfig serviceConfig;
    private final OpenSubtitlesApi openSubtitlesApi;
    private final ReentrantLock authLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;
    private final AtomicInteger remainingDownloads = new AtomicInteger(-1);

    public OpenSubtitlesAuthManager(ServiceConfig serviceConfig, OpenSubtitlesApi openSubtitlesApi) {
        this.serviceConfig = serviceConfig;
        this.openSubtitlesApi = openSubtitlesApi;
    }

    public String getToken() {
        if (isTokenValid()) {
            return cachedToken;
        }

        authLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return cachedToken;
            }
            return authenticate();
        } finally {
            authLock.unlock();
        }
    }

    public int getRemainingDownloads() {
        return remainingDownloads.get();
    }

    public boolean hasDownloadsRemaining() {
        return remainingDownloads.get() != 0;
    }

    public void updateRemainingDownloads(int remaining) {
        remainingDownloads.set(remaining);
        log.info("OpenSubtitles downloads remaining: {}", remaining);
    }

    public void refreshQuota() {
        authLock.lock();
        try {
            cachedToken = null;
            tokenExpiry = null;
            remainingDownloads.set(-1);
            authenticate();
        } finally {
            authLock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry);
    }

    private String authenticate() {
        try {
            OpenSubtitlesLoginRequest request = OpenSubtitlesLoginRequest.builder()
                    .username(serviceConfig.getOpensubtitles().getUsername())
                    .password(serviceConfig.getOpensubtitles().getPassword())
                    .build();

            OpenSubtitlesLoginResponse response = openSubtitlesApi.login(getApiKey(), request);

            if (response != null && response.getToken() != null) {
                cachedToken = response.getToken();
                tokenExpiry = Instant.now().plusSeconds(TOKEN_VALIDITY_SECONDS);

                if (response.getUser() != null && remainingDownloads.get() < 0) {
                    int allowed = response.getUser().getAllowedDownloads();
                    int used = response.getUser().getDownloadsCount();
                    remainingDownloads.set(allowed - used);
                    log.info("OpenSubtitles quota - allowed: {}, used: {}, remaining: {}",
                            allowed, used, remainingDownloads.get());
                }

                log.debug("Successfully authenticated with OpenSubtitles");
                return cachedToken;
            } else {
                log.error("OpenSubtitles authentication failed - no token received");
                return null;
            }
        } catch (Exception e) {
            log.error("OpenSubtitles authentication failed", e);
            return null;
        }
    }

    private String getApiKey() {
        return serviceConfig.getOpensubtitles().getApiKey();
    }
}
