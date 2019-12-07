package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.MediaService;
import com.github.rahmnathan.localmovie.data.Duration;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Service
@Transactional
@AllArgsConstructor
@ConditionalOnProperty(name = "service.repository.enabled", havingValue = "true")
public class MediaRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaRepositoryMonitor.class.getName());
    private final MediaFileRepository mediaFileRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final ServiceConfig serviceConfig;

    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void checkForEmptyValues() {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());
        ServiceConfig.MediaRepositoryMonitorConfig config = serviceConfig.getRepository();
        Duration updateFrequency = config.getUpdateFrequency();
        int updateLimit = config.getUpdateLimit();
        logger.info("Performing update of existing media. Frequency unit: {} value: {} Update limit: {}", updateFrequency.getUnit(), updateFrequency.getValue(), updateLimit);

        LocalDateTime queryCutoff = LocalDateTime.now().minus(config.getUpdateFrequency().getValue(), config.getUpdateFrequency().getUnit());
        mediaFileRepository.findAllByUpdatedBeforeOrderByUpdated(queryCutoff, PageRequest.of(0, updateLimit))
                .forEach(mediaFile -> {
                    logger.info("Updating media at path: {}", mediaFile.getPath());
                    Media newMedia = mediaService.loadNewMedia(mediaFile.getPath());

                    Media oldMedia = mediaFile.getMedia();
                    oldMedia.setMediaFile(null);
                    mediaRepository.delete(oldMedia);

                    newMedia.setMediaFile(mediaFile);
                    mediaFile.setMedia(newMedia);

                    mediaFileRepository.save(mediaFile);
                });

        logger.info("Update of existing media completed successfully.");
        MDC.clear();
    }
}
