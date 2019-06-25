package com.github.rahmnathan.localmovie.media.manager.persistence.control;

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
    private final MediaFileRepository mediaFileRepository;
    private final MediaDataService mediaDataService;
    private final MediaCacheService cacheService;

    public MediaRepositoryMonitor(MediaFileRepository mediaFileRepository, MediaCacheService cacheService, MediaDataService mediaDataService) {
        this.mediaDataService = mediaDataService;
        this.mediaFileRepository = mediaFileRepository;
        this.cacheService = cacheService;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void checkForEmptyValues() {
        logger.info("Performing update of existing media.");

        mediaFileRepository.findAllByUpdatedBefore(LocalDateTime.now().minusDays(3)).forEach(mediaFile -> {
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
