package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.MediaService;
import com.github.rahmnathan.localmovie.data.Duration;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
@ConditionalOnProperty(name = "service.repository.enabled", havingValue = "true")
public class MediaRepositoryMonitor {
    private final MediaFileRepository mediaFileRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final ServiceConfig serviceConfig;

    @Timed(value = "empty_value_check")
    @Scheduled(cron = "@midnight")
    public void checkForEmptyValues() {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());
        ServiceConfig.MediaRepositoryMonitorConfig config = serviceConfig.getRepository();
        Duration updateFrequency = config.getUpdateFrequency();
        int updateLimit = config.getUpdateLimit();
        log.info("Performing update of existing media. Frequency unit: {} value: {} Update limit: {}", updateFrequency.getUnit(), updateFrequency.getValue(), updateLimit);

        LocalDateTime queryCutoff = LocalDateTime.now().minus(config.getUpdateFrequency().getValue(), config.getUpdateFrequency().getUnit());
        mediaFileRepository.findAllByUpdatedBeforeOrderByUpdated(queryCutoff, PageRequest.of(0, updateLimit))
                .forEach(mediaFile -> {
                    log.info("Updating media at path: {}", mediaFile.getPath());
                    Media newMedia = mediaService.loadNewMedia(mediaFile.getPath());
                    if(newMedia.getImage() != null && newMedia.getImage().length > 0) {
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
    }
}
