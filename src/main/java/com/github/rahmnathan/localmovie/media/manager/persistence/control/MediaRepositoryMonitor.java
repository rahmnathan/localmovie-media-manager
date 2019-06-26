package com.github.rahmnathan.localmovie.media.manager.persistence.control;

import com.github.rahmnathan.localmovie.media.manager.config.MediaManagerConfig;
import com.github.rahmnathan.localmovie.media.manager.control.MediaCacheService;
import com.github.rahmnathan.localmovie.media.manager.control.MediaDataService;
import com.github.rahmnathan.localmovie.media.manager.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.persistence.repository.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class MediaRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaRepositoryMonitor.class.getName());
    private final MediaManagerConfig.MediaRepositoryMonitorConfig repositoryMonitorConfig;
    private final MediaFileRepository mediaFileRepository;
    private final MediaDataService mediaDataService;
    private final MediaCacheService cacheService;

    public MediaRepositoryMonitor(MediaFileRepository mediaFileRepository,
                                  MediaCacheService cacheService,
                                  MediaDataService mediaDataService,
                                  MediaManagerConfig mediaManagerConfig) {
        this.repositoryMonitorConfig = mediaManagerConfig.getRepository();
        this.mediaFileRepository = mediaFileRepository;
        this.mediaDataService = mediaDataService;
        this.cacheService = cacheService;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void checkForEmptyValues() {
        int updateFrequencyDays = repositoryMonitorConfig.getUpdateFrequencyDays();
        int updateLimit = repositoryMonitorConfig.getUpdateLimit();
        logger.info("Performing update of existing media. Frequency days: {} Update limit: {}", updateFrequencyDays, updateLimit);

        LocalDateTime queryCutoff = LocalDateTime.now().minusDays(updateFrequencyDays);
        mediaFileRepository.findAllByUpdatedBeforeOrderByUpdated(queryCutoff).stream()
                .limit(updateLimit)
                .forEach(mediaFile -> {
                    try {
                        logger.info("Updating media at path: {}", mediaFile.getPath());
                        MediaFile updatedMediaFile = mediaDataService.loadNewMediaFile(mediaFile.getPath());
                        mediaFile.setMedia(updatedMediaFile.getMedia());

                        mediaFileRepository.save(mediaFile);
                        cacheService.addMedia(updatedMediaFile);
                    } catch (InvalidMediaException e) {
                        logger.error("Failure loading media data.", e);
                    }
                });
    }
}
