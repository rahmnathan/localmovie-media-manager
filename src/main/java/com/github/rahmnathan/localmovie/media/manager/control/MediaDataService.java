package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.manager.repository.MediaRepository;
import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import com.github.rahmnathan.omdb.data.Media;
import com.github.rahmnathan.omdb.data.MediaType;
import com.github.rahmnathan.omdb.exception.MediaProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

import static com.github.rahmnathan.localmovie.media.manager.control.PathUtils.*;

@Service
public class MediaDataService {
    private final Logger logger = LoggerFactory.getLogger(MediaDataService.class.getName());
    private final OmdbMediaProvider mediaProvider;
    private final MediaRepository repository;

    public MediaDataService(MediaRepository repository, OmdbMediaProvider mediaProvider) {
        this.mediaProvider = mediaProvider;
        this.repository = repository;
    }

    public MediaFile loadMediaFile(String path) throws InvalidMediaException {
        Optional<MediaFile> mediaFile = repository.findById(path);
        if (mediaFile.isPresent()) {
            logger.info("Getting from database - {}", path);
            return mediaFile.get();
        }

        return loadNewMediaFile(path);
    }

    public MediaFile loadNewMediaFile(String path) throws InvalidMediaException {
        MediaFile.Builder mediaFileBuilder = MediaFile.Builder.forPath(path);

        Media media;
        if (isTopLevel(path) || isEpisode(path)) {
            media = loadMediaFromProvider(path);
        } else {
            media = loadSeriesParentInfo(path, MediaType.SEASON);
        }

        mediaFileBuilder.setMedia(media);
        return mediaFileBuilder.build();
    }

    public MediaFile saveMediaFile(MediaFile mediaFile){
        return repository.save(mediaFile);
    }

    public boolean existsInDatabase(String path){
        return repository.existsById(path);
    }

    private Media loadMediaFromProvider(String path) throws InvalidMediaException {
        logger.info("Loading MediaFile from provider - {}", path);
        String fileName = new File(path).getName();

        if(isEpisode(path)){
            try {
                return loadEpisodeFromProvider(path, fileName);
            } catch (MediaProviderException e) {
                logger.error("Error getting media from provider", e);
                return loadSeriesParentInfo(path, MediaType.EPISODE);
            }
        } else {
            String title = getTitle(fileName);

            try {
                return mediaProvider.getMovie(title);
            } catch (MediaProviderException e) {
                logger.error("Error getting media from provider", e);
                return Media.Builder.newInstance().setTitle(title).build();
            }
        }
    }

    private Media loadEpisodeFromProvider(String path, String fileName) throws MediaProviderException, InvalidMediaException {
        String seriesTitle = getParentFile(path).getName();
        int seasonNumber = getSeasonNumber(path);
        int episodeNumber = getEpisodeNumber(fileName);

        return mediaProvider.getEpisode(seriesTitle, seasonNumber, episodeNumber);
    }

    private Media loadSeriesParentInfo(String path, MediaType mediaType) throws InvalidMediaException {
        logger.info("Getting info from parent - {}", path);

        String filename = new File(path).getName();
        File file = getParentFile(path);
        logger.info("{} - Parent resolved to: {}", path, file.getPath());

        MediaFile parentInfo = loadMediaFile(file.getPath());
        Integer number = isEpisode(path) ? getEpisodeNumber(filename) : getSeasonNumber(filename);
        return Media.Builder.copyWithNewTitleNumberAndType(parentInfo.getMedia(), getTitle(filename), number, mediaType);
    }
}
