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
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;
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

        Set<String> mediaWithMissingFields = findMediaWithMissingData();
        updateMedia(mediaWithMissingFields);
    }

    public void updateMedia(Set<String> mediaPaths){
        mediaPaths.forEach(mediaPath -> {
            logger.info("Updating media at path: {}", mediaPath);

            deleteMedia(mediaPath);

            MediaFile newMediaFile = mediaDataService.loadUpdatedMediaFile(mediaPath);
            cacheService.addMedia(newMediaFile);
        });
    }

    @Transactional
    public void deleteMedia(String path){
        mediaRepository.deleteById(path);
    }

    @Transactional
    public Set<String> findMediaWithMissingData() {
        return StreamSupport.stream(mediaRepository.findAll().spliterator(), true)
                .filter(mediaFile -> mediaFile.getMedia().hasMissingValues() || (mediaFile.getMedia().getMediaType() == MediaType.MOVIE && isEpisode(mediaFile.getPath())))
                .peek(mediaFile -> logger.info("Detected missing fields: {}", mediaFile.toString()))
                .map(MediaFile::getPath)
                .collect(Collectors.toSet());
    }
}
