package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.control.MediaCacheService;
import com.github.rahmnathan.localmovie.media.manager.control.MediaDataService;
import com.github.rahmnathan.omdb.data.Media;
import com.github.rahmnathan.omdb.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.stream.StreamSupport;

import static com.github.rahmnathan.localmovie.media.manager.control.PathUtils.isEpisode;

@Service
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

    @Scheduled(fixedDelay = 3600000, initialDelay = 30000)
    public void checkForEmptyValues(){
        logger.info("Checking for null MovieInfo fields in database.");

        StreamSupport.stream(mediaRepository.findAll().spliterator(), true).forEach(mediaFile -> {
            Media existingMedia = mediaFile.getMedia();
            if(existingMedia.hasMissingValues() || existingMedia.getMediaType() == MediaType.MOVIE && isEpisode(mediaFile.getPath())) {
                logger.info("Detected missing fields: {}", existingMedia.toString());

                MediaFile newMediaFile = mediaDataService.loadUpdatedMediaFile(mediaFile.getPath());
                cacheService.addMedia(newMediaFile);
            }
        });
    }
}
