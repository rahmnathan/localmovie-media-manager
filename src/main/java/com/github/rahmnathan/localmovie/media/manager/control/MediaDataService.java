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

    MediaFile loadMediaFile(String path) throws InvalidMediaException {
        Optional<MediaFile> mediaFile = repository.findById(path);
        if (mediaFile.isPresent()) {
            logger.info("Getting from database - {}", path);
            return mediaFile.get();
        }

        return loadUpdatedMediaFile(path);
    }

    public MediaFile loadUpdatedMediaFile(String path) throws InvalidMediaException {
        if (isTopLevel(path) || isEpisode(path)) {
            return loadMediaInfoFromProvider(path);
        } else {
            return loadSeriesParentInfo(path, MediaType.SEASON);
        }
    }

    public void saveMediaFile(MediaFile mediaFile){
        repository.save(mediaFile);
    }

    public boolean existsInDatabase(String path){
        return repository.existsById(path);
    }

    public void deleteMediaFile(String path){
        repository.deleteById(path);
    }

    private MediaFile loadMediaInfoFromProvider(String path) throws InvalidMediaException {
        logger.info("Loading MediaFile from provider - {}", path);
        String fileName = new File(path).getName();
        String title = getTitle(fileName);

        MediaFile.Builder builder = MediaFile.Builder.newInstance()
                .setFileName(fileName)
                .setPath(path)
                .setMedia(Media.Builder.newInstance().setTitle(title).build())
                .setViews(0);

        if(isEpisode(path)){
            try {
                String seriesTitle = getParentFile(path).getName();
                int seasonNumber = getSeasonNumber(path);
                int episodeNumber = getEpisodeNumber(fileName);

                builder.setMedia(mediaProvider.getEpisode(seriesTitle, seasonNumber, episodeNumber));
            } catch (MediaProviderException e) {
                logger.error("Error getting media from provider", e);
                return loadSeriesParentInfo(path, MediaType.EPISODE);
            }
        } else {
            try {
                builder.setMedia(mediaProvider.getMovie(title));
            } catch (MediaProviderException e) {
                logger.error("Error getting media from provider", e);
            }
        }

        return builder.build();
    }

    private MediaFile loadSeriesParentInfo(String path, MediaType mediaType) throws InvalidMediaException {
        logger.info("Getting info from parent - {}", path);

        String filename = new File(path).getName();
        File file = getParentFile(path);
        logger.info("{} - Parent resolved to: {}", path, file.getPath());

        MediaFile parentInfo = loadMediaFile(file.getPath());
        Integer number = isEpisode(path) ? getEpisodeNumber(filename) : getSeasonNumber(filename);
        return MediaFile.Builder.copyWithNewTitle(parentInfo, filename, getTitle(filename), path, number, mediaType);
    }
}
