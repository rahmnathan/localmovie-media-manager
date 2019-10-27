package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import com.github.rahmnathan.omdb.data.MediaType;
import com.github.rahmnathan.omdb.exception.MediaProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Optional;

import static com.github.rahmnathan.localmovie.control.PathUtils.*;

@Service
public class MediaDataService {
    private final Logger logger = LoggerFactory.getLogger(MediaDataService.class.getName());
    private final OmdbMediaProvider mediaProvider;
    private final MediaFileRepository repository;

    public MediaDataService(MediaFileRepository repository, OmdbMediaProvider mediaProvider) {
        this.mediaProvider = mediaProvider;
        this.repository = repository;
    }

    @Transactional
    public Media loadMedia(String path) {
        Optional<MediaFile> mediaFile = repository.findByPath(path);
        if (mediaFile.isPresent()) {
            logger.info("Getting from database - {}", path);
            return mediaFile.get().getMedia();
        }

        return loadNewMediaFile(path);
    }

    public Media loadNewMediaFile(String path) {
        com.github.rahmnathan.omdb.data.Media media;
        if (isTopLevel(path) || isEpisode(path)) {
            media = loadMediaFromProvider(path);
        } else {
            media = loadSeriesParentInfo(path, MediaType.SEASON);
        }

        return com.github.rahmnathan.localmovie.persistence.entity.Media.fromOmdbMedia(media);
    }

    public MediaFile saveMediaFile(MediaFile mediaFile){
        return repository.save(mediaFile);
    }

    public boolean existsInDatabase(String path){
        return repository.existsByPath(path);
    }

    private com.github.rahmnathan.omdb.data.Media loadMediaFromProvider(String path) {
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
                return com.github.rahmnathan.omdb.data.Media.builder().title(title).build();
            }
        }
    }

    private com.github.rahmnathan.omdb.data.Media loadEpisodeFromProvider(String path, String fileName) throws MediaProviderException {
        String seriesTitle = getParentFile(path).getName();
        int seasonNumber = parseSeasonNumber(path);
        int episodeNumber = parseEpisodeNumber(fileName);

        return mediaProvider.getEpisode(seriesTitle, seasonNumber, episodeNumber);
    }

    private com.github.rahmnathan.omdb.data.Media loadSeriesParentInfo(String path, MediaType mediaType) {
        logger.info("Getting info from parent - {}", path);

        String filename = new File(path).getName();
        File file = getParentFile(path);
        logger.info("{} - Parent resolved to: {}", path, file.getPath());

        Media parentInfo = loadMedia(file.getPath());
        Integer number = isEpisode(path) ? parseEpisodeNumber(filename) : parseSeasonNumber(filename);
        return com.github.rahmnathan.omdb.data.Media.copyWithNewTitleNumberAndType(parentInfo.toOmdbMedia(), getTitle(filename), number, mediaType);
    }
}
