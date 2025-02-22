package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.omdb.boundary.MediaProvider;
import com.github.rahmnathan.omdb.data.MediaType;
import com.github.rahmnathan.omdb.exception.MediaProviderException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

import static com.github.rahmnathan.localmovie.media.PathUtils.*;

@Slf4j
@Service
@AllArgsConstructor
public class MediaService {
    private final MediaPersistenceService persistenceService;
    private final MediaProvider mediaProvider;

    @Transactional
    public Media loadMedia(String path) {
        return loadMediaInternal(path);
    }

    private Media loadMediaInternal(String path) {
        try {
            if (isTopLevel(path) || isEpisode(path)) {
                return Media.fromOmdbMedia(loadMediaFromProvider(path));
            } else {
                return Media.fromOmdbMedia(loadSeriesParentInfo(path, MediaType.SEASON));
            }
        } catch (MediaProviderException | InvalidMediaException e){
            return new Media(getTitle(new File(path).getName()));
        }
    }

    public boolean existsInDatabase(String path){
        return persistenceService.existsByPath(path);
    }

    private com.github.rahmnathan.omdb.data.Media loadMediaFromProvider(String path) throws InvalidMediaException, MediaProviderException {
        log.info("Loading MediaFile from provider - {}", path);
        String fileName = new File(path).getName();

        if(isEpisode(path)){
            try {
                return loadEpisodeFromProvider(path, fileName);
            } catch (MediaProviderException e) {
                log.error("Error getting media from provider", e);
                return loadSeriesParentInfo(path, MediaType.EPISODE);
            }
        } else if (isSeries(path)){
            String title = getTitle(fileName);
            return mediaProvider.getSeries(title);
        } else {
            String title = getTitle(fileName);
            return mediaProvider.getMovie(title);
        }
    }

    private com.github.rahmnathan.omdb.data.Media loadEpisodeFromProvider(String path, String fileName) throws MediaProviderException, InvalidMediaException {
        String seriesTitle = getParentFile(path).getName();
        int seasonNumber = parseSeasonNumber(path);
        int episodeNumber = parseEpisodeNumber(fileName);

        return mediaProvider.getEpisode(seriesTitle, seasonNumber, episodeNumber);
    }

    private com.github.rahmnathan.omdb.data.Media loadSeriesParentInfo(String path, MediaType mediaType) throws InvalidMediaException {
        log.info("Getting info from parent - {}", path);

        String filename = new File(path).getName();
        File file = getParentFile(path);
        log.info("{} - Parent resolved to: {}", path, file.getPath());

        Media parentInfo = loadMediaInternal(file.getPath());
        Integer number = isEpisode(path) ? parseEpisodeNumber(filename) : parseSeasonNumber(filename);
        return com.github.rahmnathan.omdb.data.Media.copyWithNewTitleNumberAndType(parentInfo.toOmdbMedia(), getTitle(filename), number, mediaType);
    }
}
