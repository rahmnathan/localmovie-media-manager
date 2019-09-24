package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.MediaDataService;
import com.github.rahmnathan.localmovie.data.Duration;
import com.github.rahmnathan.localmovie.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class MediaRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaRepositoryMonitor.class.getName());
    private final ServiceConfig.MediaRepositoryMonitorConfig config;
    private final MediaFileRepository mediaFileRepository;
    private final MediaDataService mediaDataService;
    private final MediaRepository mediaRepository;

    public MediaRepositoryMonitor(MediaFileRepository mediaFileRepository,
                                  MediaRepository mediaRepository,
                                  MediaDataService mediaDataService,
                                  ServiceConfig serviceConfig) {
        this.config = serviceConfig.getRepository();
        this.mediaFileRepository = mediaFileRepository;
        this.mediaDataService = mediaDataService;
        this.mediaRepository = mediaRepository;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void checkForEmptyValues() {
        Duration updateFrequency = config.getUpdateFrequency();
        int updateLimit = config.getUpdateLimit();
        logger.info("Performing update of existing media. Frequency unit: {} value: {} Update limit: {}", updateFrequency.getUnit(), updateFrequency.getValue(), updateLimit);

        LocalDateTime queryCutoff = LocalDateTime.now().minus(config.getUpdateFrequency().getValue(), config.getUpdateFrequency().getUnit());
        mediaFileRepository.findAllByUpdatedBeforeOrderByUpdated(queryCutoff, PageRequest.of(0, updateLimit))
                .forEach(mediaFile -> {
                    try {
                        logger.info("Updating media at path: {}", mediaFile.getPath());
                        MediaFile newMediaFile = mediaDataService.loadNewMediaFile(mediaFile.getPath());

                        Media oldMedia = mediaFile.getMedia();
                        oldMedia.setMediaFile(null);
                        mediaRepository.delete(oldMedia);

                        Media newMedia = newMediaFile.getMedia();
                        newMedia.setMediaFile(mediaFile);
                        mediaFile.setMedia(newMedia);

                        mediaFileRepository.save(mediaFile);
                    } catch (InvalidMediaException e) {
                        logger.error("Failure loading media data.", e);
                    }
                });
    }
}
