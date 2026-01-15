package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.omdb.MediaProvider;
import com.github.rahmnathan.localmovie.media.omdb.MediaProviderException;
import com.github.rahmnathan.localmovie.media.omdb.MediaType;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class MediaService {
    private final MediaPersistenceService persistenceService;
    private final MediaProvider mediaProvider;

    @Transactional
    public Media loadMedia(MediaPath path) {
        try {
            return loadMediaFromProvider(path);
        } catch (MediaProviderException | InvalidMediaException e){
            return new Media(path.getTitle());
        }
    }

    public boolean existsInDatabase(String path){
        return persistenceService.existsByPath(path);
    }

    private Media loadMediaFromProvider(MediaPath path) throws InvalidMediaException, MediaProviderException {
        log.info("Loading media for path: {}", path);

        if (path.getMediaType() == null) {
            throw new InvalidMediaException("Media can't be loaded for path: " + path);
        }

        return switch (path.getMediaType()) {
            case MOVIE -> mediaProvider.getMovie(path.getTitle(), path.getReleaseYear());
            case SERIES -> mediaProvider.getSeries(path.getTitle(), path.getReleaseYear());
            case SEASON -> loadSeriesParentInfo(path);
            case EPISODE -> {
                try {
                    yield mediaProvider.getEpisode(path.getSeriesPath().getTitle(), path.getSeasonNumber(), path.getEpisodeNumber());
                } catch (MediaProviderException e) {
                    log.error("Failed to load episode from MediaProvider.", e);
                    yield loadSeriesParentInfo(path);
                }
            }
        };
    }

    private Media loadSeriesParentInfo(MediaPath path) {
        log.info("Getting info from parent - {}", path);

        Media parentInfoClone = loadMedia(path.getSeriesPath()).cloneToPojo();

        parentInfoClone.setNumber(path.getMediaType() == MediaType.EPISODE ? path.getEpisodeNumber() : path.getSeasonNumber());
        parentInfoClone.setTitle(parentInfoClone.getTitle());
        parentInfoClone.setMediaType(path.getMediaType());

        return parentInfoClone;
    }
}
