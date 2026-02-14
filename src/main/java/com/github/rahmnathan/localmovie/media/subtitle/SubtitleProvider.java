package com.github.rahmnathan.localmovie.media.subtitle;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.*;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesDownloadRequest;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesDownloadResponse;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesSearchResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component
public class SubtitleProvider {
    private static final Logger logger = LoggerFactory.getLogger(SubtitleProvider.class);
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String VTT_FORMAT = "webvtt";

    private final Client client;
    private final OpenSubtitlesApi openSubtitlesApi;
    private final OpenSubtitlesAuthManager authManager;
    private final ServiceConfig serviceConfig;

    public SubtitleProvider(ServiceConfig serviceConfig, Client client,
                            OpenSubtitlesApi openSubtitlesApi, OpenSubtitlesAuthManager authManager) {
        this.serviceConfig = serviceConfig;
        this.client = client;
        this.openSubtitlesApi = openSubtitlesApi;
        this.authManager = authManager;
    }

    public int getRemainingDownloads() {
        return authManager.getRemainingDownloads();
    }

    public boolean hasDownloadsRemaining() {
        return authManager.hasDownloadsRemaining();
    }

    public void refreshQuota() {
        authManager.refreshQuota();
    }

    public Optional<SubtitleResult> fetchSubtitle(String imdbId) throws DownloadQuotaExceededException {
        if (!serviceConfig.getOpensubtitles().isEnabled()) {
            logger.debug("OpenSubtitles integration disabled");
            return Optional.empty();
        }

        if (!authManager.hasDownloadsRemaining()) {
            logger.warn("OpenSubtitles download quota exhausted");
            throw new DownloadQuotaExceededException();
        }

        String token = authManager.getToken();
        if (token == null) {
            logger.warn("Failed to authenticate with OpenSubtitles");
            return Optional.empty();
        }

        return searchAndDownload(imdbId, token);
    }

    private Optional<SubtitleResult> searchAndDownload(String imdbId, String token) throws DownloadQuotaExceededException {
        OpenSubtitlesSearchResponse.SubtitleData subtitle = searchSubtitle(imdbId, token);
        if (subtitle == null) {
            return Optional.empty();
        }

        return downloadSubtitle(subtitle, imdbId, token);
    }

    private OpenSubtitlesSearchResponse.SubtitleData searchSubtitle(String imdbId, String token) {
        OpenSubtitlesSearchResponse searchResponse = openSubtitlesApi.search(
                getApiKey(),
                "Bearer " + token,
                normalizeImdbId(imdbId),
                DEFAULT_LANGUAGE);

        if (searchResponse == null || !searchResponse.hasResults()) {
            logger.info("No subtitles found for IMDB ID: {}", imdbId);
            return null;
        }

        // Select best subtitle (highest download count)
        return searchResponse.getData().stream()
                .filter(d -> d.getAttributes() != null
                        && d.getAttributes().getFiles() != null
                        && !d.getAttributes().getFiles().isEmpty())
                .max(Comparator.comparingInt(a -> a.getAttributes().getDownloadCount()))
                .orElse(null);
    }

    private Optional<SubtitleResult> downloadSubtitle(OpenSubtitlesSearchResponse.SubtitleData subtitle,
                                                       String imdbId, String token) {
        int fileId = subtitle.getAttributes().getFiles().getFirst().getFileId();
        String subtitleId = subtitle.getAttributes().getSubtitleId();

        OpenSubtitlesDownloadRequest downloadRequest = OpenSubtitlesDownloadRequest.builder()
                .fileId(fileId)
                .subFormat(VTT_FORMAT)
                .build();

        OpenSubtitlesDownloadResponse downloadResponse = openSubtitlesApi.download(
                getApiKey(),
                "Bearer " + token,
                downloadRequest);

        if (downloadResponse == null || downloadResponse.getLink() == null) {
            logger.warn("Failed to get download link for subtitle: {}", subtitleId);
            return Optional.empty();
        }

        authManager.updateRemainingDownloads(downloadResponse.getRemaining());

        String vttContent = fetchSubtitleContent(downloadResponse.getLink());
        if (vttContent == null) {
            return Optional.empty();
        }

        logger.info("Successfully fetched subtitle for IMDB ID: {} (OpenSubtitles ID: {})", imdbId, subtitleId);

        return Optional.of(SubtitleResult.builder()
                .content(vttContent)
                .opensubtitlesId(subtitleId)
                .languageCode(DEFAULT_LANGUAGE)
                .format("vtt")
                .build());
    }

    private String fetchSubtitleContent(String downloadUrl) {
        try {
            Response response = client.target(downloadUrl)
                    .request()
                    .get();

            if (response.getStatus() == 200) {
                return response.readEntity(String.class);
            } else {
                logger.warn("Failed to download subtitle from {}. Status: {}",
                        downloadUrl, response.getStatus());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Failed to download subtitle: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeImdbId(String imdbId) {
        if (imdbId == null) return null;
        return imdbId.startsWith("tt") ? imdbId.substring(2) : imdbId;
    }

    private String getApiKey() {
        return serviceConfig.getOpensubtitles().getApiKey();
    }
}
