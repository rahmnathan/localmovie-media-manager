package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.domain.MediaFileEvent;
import com.github.rahmnathan.localmovie.domain.MovieEvent;
import com.github.rahmnathan.localmovie.media.manager.control.MediaCacheService;
import com.github.rahmnathan.localmovie.media.manager.control.MediaDataService;
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
@Transactional
public class MovieRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MovieRepositoryMonitor.class.getName());
    private final MovieRepository mediaRepository;
    private final MediaDataService mediaDataService;
    private final MediaEventRepository eventRepository;
    private final MediaCacheService cacheService;

    public MovieRepositoryMonitor(MovieRepository movieRepository, MediaEventRepository eventRepository, MediaCacheService cacheService, MediaDataService mediaDataService) {
        this.mediaDataService = mediaDataService;
        this.mediaRepository = movieRepository;
        this.cacheService = cacheService;
        this.eventRepository = eventRepository;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 30000)
    public void checkForEmptyValues(){
        logger.info("Checking for null MovieInfo fields in database.");

        Set<String> mediaWithMissingFields = findMediaWithMissingData();
        updateMedia(mediaWithMissingFields);
    }

    public void updateMedia(Set<String> mediaPaths) {
        mediaPaths.forEach(mediaPath -> {
            logger.info("Updating media at path: {}", mediaPath);

            deleteMediaEvents(mediaPath);
            deleteMedia(mediaPath);

            MediaFile newMediaFile = mediaDataService.loadUpdatedMediaFile(mediaPath);
            cacheService.addMedia(newMediaFile);

            addUpdateEvent(mediaPath, newMediaFile);
        });
    }

    private void addUpdateEvent(String resultFilePath, MediaFile mediaFile){
        logger.info("Adding update event to repository.");
        MediaFileEvent event = new MediaFileEvent(MovieEvent.ENTRY_CREATE.getMovieEventString(), mediaFile, resultFilePath);
        cacheService.addEvent(eventRepository.saveAndFlush(event));
    }

    public void deleteMediaEvents(String path){
        eventRepository.deleteAllByRelativePath(path);
    }

    public void deleteMedia(String path){
        if(mediaRepository.existsById(path)) {
            mediaRepository.deleteById(path);
        }
    }

    public Set<String> findMediaWithMissingData() {
        return StreamSupport.stream(mediaRepository.findAll().spliterator(), true)
                .filter(mediaFile -> mediaFile.getMedia().hasMissingValues() || (mediaFile.getMedia().getMediaType() == MediaType.MOVIE && isEpisode(mediaFile.getPath())))
                .peek(mediaFile -> logger.info("Detected missing fields for media: {}", mediaFile.getPath()))
                .map(MediaFile::getPath)
                .collect(Collectors.toSet());
    }
}
