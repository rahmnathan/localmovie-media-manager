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
import java.util.List;

@Service
@Transactional
public class MovieRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MovieRepositoryMonitor.class.getName());
    private final MovieRepository mediaRepository;
    private final MediaDataService mediaDataService;
    private final MediaCacheService cacheService;

    public MovieRepositoryMonitor(MovieRepository movieRepository, MediaCacheService cacheService, MediaDataService mediaDataService) {
        this.mediaDataService = mediaDataService;
        this.mediaRepository = movieRepository;
        this.cacheService = cacheService;
    }

    @Scheduled(fixedDelay = 86400000, initialDelay = 30000)
    public void checkForEmptyValues() {
        logger.info("Performing update of existing media.");

        findStaleMedia().forEach(mediaFile -> {
            try {
                MediaFile newMediaFile = mediaDataService.loadUpdatedMediaFile(mediaFile.getPath());
                cacheService.addMedia(newMediaFile);
            } catch (InvalidMediaException e) {
                logger.error("Failure loading media data.", e);
            }
        });
    }

    public List<MediaFile> findStaleMedia() {
        return mediaRepository.findAllByUpdatedBefore(LocalDateTime.now().minusDays(3));
    }
}
