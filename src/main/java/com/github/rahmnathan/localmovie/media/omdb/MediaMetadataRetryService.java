package com.github.rahmnathan.localmovie.media.omdb;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.subtitle.SubtitleJobService;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.rahmnathan.localmovie.web.filter.LoggingFilter.X_CORRELATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMetadataRetryService {
    private static final int BATCH_SIZE = 10;

    private final MediaFileRepository mediaFileRepository;
    private final MediaRepository mediaRepository;
    private final MediaProvider mediaProvider;
    private final SubtitleJobService subtitleJobService;
    private final ServiceConfig serviceConfig;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${service.omdb.retryDelayMs:3600000}")
    @SchedulerLock(name = "retry-missing-imdb-ids-lock", lockAtMostFor = "PT30M")
    @Transactional
    public void retryMissingImdbIds() {
        if (!serviceConfig.getOmdb().isEnabled()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        try {
            List<MediaFile> mediaFiles = mediaFileRepository
                    .findStreamableMediaFilesMissingImdbId(PageRequest.of(0, BATCH_SIZE));

            if (mediaFiles.isEmpty()) {
                return;
            }

            log.info("Found {} media files missing IMDB IDs, attempting to fetch metadata", mediaFiles.size());

            int successCount = 0;
            for (MediaFile mediaFile : mediaFiles) {
                if (retryMetadataFetch(mediaFile)) {
                    successCount++;
                }
            }

            log.info("Successfully fetched IMDB IDs for {}/{} media files", successCount, mediaFiles.size());
            meterRegistry.counter("localmovies.metadata-retry.success").increment(successCount);
            meterRegistry.counter("localmovies.metadata-retry.failed").increment(mediaFiles.size() - successCount);

        } finally {
            MDC.clear();
            meterRegistry.timer("localmovies.metadata-retry.process")
                    .record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
        }
    }

    private boolean retryMetadataFetch(MediaFile mediaFile) {
        String absolutePath = mediaFile.getAbsolutePath();
        log.debug("Retrying metadata fetch for: {}", absolutePath);

        try {
            MediaPath mediaPath = MediaPath.parse(absolutePath);
            Media newMedia = fetchMediaFromProvider(mediaPath);

            if (newMedia == null || newMedia.getImdbId() == null || newMedia.getImdbId().isBlank()) {
                log.debug("Still no IMDB ID available for: {}", absolutePath);
                return false;
            }

            Media oldMedia = mediaFile.getMedia();
            mediaFile.setMedia(newMedia);
            mediaFileRepository.save(mediaFile);
            mediaRepository.delete(oldMedia);

            log.info("Successfully fetched IMDB ID {} for: {}", newMedia.getImdbId(), absolutePath);

            queueSubtitleFetch(mediaFile, newMedia.getImdbId());

            return true;

        } catch (InvalidMediaException e) {
            log.warn("Invalid media path, skipping: {}", absolutePath);
            return false;
        } catch (Exception e) {
            log.warn("Failed to fetch metadata for: {} - {}", absolutePath, e.getMessage());
            return false;
        }
    }

    private Media fetchMediaFromProvider(MediaPath path) {
        if (path.getMediaType() == null) {
            return null;
        }

        try {
            return switch (path.getMediaType()) {
                case MOVIE -> mediaProvider.getMovie(path.getTitle(), path.getReleaseYear());
                case SERIES -> mediaProvider.getSeries(path.getTitle(), path.getReleaseYear());
                case SEASON, EPISODE -> fetchSeriesImdbId(path);
            };
        } catch (MediaProviderException e) {
            log.debug("MediaProvider lookup failed for {}: {}", path.getTitle(), e.getMessage());
            return null;
        }
    }

    private Media fetchSeriesImdbId(MediaPath path) {
        try {
            Media seriesMedia = mediaProvider.getSeries(path.getSeriesPath().getTitle(), path.getSeriesPath().getReleaseYear());
            if (path.getMediaType() == MediaType.EPISODE) {
                Media episodeMedia = mediaProvider.getEpisode(
                        path.getSeriesPath().getTitle(),
                        path.getSeasonNumber(),
                        path.getEpisodeNumber());
                return episodeMedia;
            }
            seriesMedia.setNumber(path.getSeasonNumber());
            seriesMedia.setTitle("Season " + path.getSeasonNumber());
            seriesMedia.setMediaType(path.getMediaType());
            return seriesMedia;
        } catch (MediaProviderException e) {
            log.debug("Series lookup failed for {}: {}", path.getSeriesPath().getTitle(), e.getMessage());
            return null;
        }
    }

    private void queueSubtitleFetch(MediaFile mediaFile, String imdbId) {
        if (!serviceConfig.getOpensubtitles().isEnabled()) {
            return;
        }

        subtitleJobService.queueSubtitleFetch(mediaFile, imdbId);
        log.info("Queued subtitle fetch for media file {} with IMDB ID {}",
                mediaFile.getMediaFileId(), imdbId);
    }
}
