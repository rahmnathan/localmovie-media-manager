package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.Duration;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MediaUpdateService {
    private final MediaFileRepository mediaFileRepository;
    private final MediaRepository mediaRepository;
    private final ServiceConfig serviceConfig;
    private final MeterRegistry meterRegistry;
    private final MediaService mediaService;

    // disabled for now in favor of new manual update endpoint
//    @Scheduled(cron = "@midnight")
    public void checkForEmptyValues() {
        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        ServiceConfig.MediaRepositoryMonitorConfig config = serviceConfig.getRepository();
        Duration updateFrequency = config.getUpdateFrequency();
        int updateLimit = config.getUpdateLimit();
        log.info("Performing update of existing media. Frequency unit: {} value: {} Update limit: {}", updateFrequency.getUnit(), updateFrequency.getValue(), updateLimit);

        LocalDateTime queryCutoff = LocalDateTime.now().minus(config.getUpdateFrequency().getValue(), config.getUpdateFrequency().getUnit());
        mediaFileRepository.findAllByUpdatedBeforeOrderByUpdated(queryCutoff, PageRequest.of(0, updateLimit))
                .forEach(mediaFile -> {
                    log.info("Updating media at path: {}", mediaFile.getPath());
                    Media newMedia = mediaService.loadMedia(mediaFile.getPath());
                    if(newMedia.getImage() != null && newMedia.getImage().getImage() != null && newMedia.getImage().getImage().length > 0) {
                        Media oldMedia = mediaFile.getMedia();
                        oldMedia.setMediaFile(null);
                        mediaRepository.delete(oldMedia);

                        newMedia.setMediaFile(mediaFile);
                        mediaFile.setMedia(newMedia);

                        mediaFileRepository.save(mediaFile);
                    }
                });

        log.info("Update of existing media completed successfully.");
        MDC.clear();
        meterRegistry.timer("localmovies.refresh-media-metadata").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    public void updateMedia(String path) {
        MediaFile mediaFile = mediaFileRepository.findByPath(path).orElseThrow();

        Media oldMedia = mediaFile.getMedia();

        mediaFile.setMedia(mediaService.loadMedia(path));
        mediaFileRepository.save(mediaFile);

        mediaRepository.delete(oldMedia);
    }
}
