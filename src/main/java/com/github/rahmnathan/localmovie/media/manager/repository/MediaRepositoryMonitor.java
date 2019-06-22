package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.control.MediaCacheService;
import com.github.rahmnathan.localmovie.media.manager.control.MediaDataService;
import com.github.rahmnathan.localmovie.media.manager.exception.InvalidMediaException;
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
    private final MediaRepository mediaRepository;
    private final MediaDataService mediaDataService;
    private final MediaCacheService cacheService;

    public MediaRepositoryMonitor(MediaRepository mediaRepository, MediaCacheService cacheService, MediaDataService mediaDataService) {
        this.mediaDataService = mediaDataService;
        this.mediaRepository = mediaRepository;
        this.cacheService = cacheService;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void checkForEmptyValues() {
        logger.info("Performing update of existing media.");

        mediaRepository.findAllByUpdatedBefore(LocalDateTime.now().minusDays(3)).forEach(mediaFile -> {
            try {
                logger.info("Updating media at path: {}", mediaFile.getPath());
                MediaFile updatedMediaFile = mediaDataService.loadNewMediaFile(mediaFile.getPath());
                mediaFile.setMedia(updatedMediaFile.getMedia());

                mediaRepository.save(mediaFile);
                cacheService.addMedia(updatedMediaFile);
            } catch (InvalidMediaException e) {
                logger.error("Failure loading media data.", e);
            }
        });
    }
}
