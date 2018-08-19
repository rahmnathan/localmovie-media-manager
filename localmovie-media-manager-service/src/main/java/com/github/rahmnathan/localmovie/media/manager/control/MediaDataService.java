package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.repository.MovieRepository;
import com.github.rahmnathan.omdb.boundary.OmdbMovieProvider;
import com.github.rahmnathan.omdb.exception.MovieProviderException;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
public class MediaDataService {
    private final Logger logger = LoggerFactory.getLogger(MediaDataService.class.getName());
    private static final Timer MOVIE_PROVIDER_TIMER = Metrics.timer("localmovies.omdb.timer");
    private final OmdbMovieProvider movieProvider;
    private final MovieRepository repository;

    public MediaDataService(MovieRepository repository, OmdbMovieProvider movieProvider) {
        this.movieProvider = movieProvider;
        this.repository = repository;
    }

    public MediaFile loadMediaFile(String path) {
        Optional<MediaFile> mediaFile = repository.findById(path);
        if (mediaFile.isPresent()) {
            logger.info("Getting from database - {}", path);
            return mediaFile.get();
        } else if (PathUtils.isTopLevel(path)) {
            return loadMediaInfoFromProvider(path);
        } else {
            return loadSeriesParentInfo(path);
        }
    }

    private MediaFile loadMediaInfoFromProvider(String path) {
        logger.info("Loading MediaFile from provider - {}", path);
        String fileName = new File(path).getName();
        String title = PathUtils.getTitle(fileName);

        MediaFile.Builder builder = MediaFile.Builder.newInstance()
                .setFileName(fileName)
                .setPath(path)
                .setViews(0);

        MOVIE_PROVIDER_TIMER.record(() -> {
            try {
                builder.setMovie(movieProvider.getMovie(title));
            } catch (MovieProviderException e){
                logger.error("Error getting movie from provider", e);
            }
        });

        MediaFile mediaFile = builder.build();
        return repository.save(mediaFile);
    }

    private MediaFile loadSeriesParentInfo(String path) {
        logger.info("Getting info from parent - {}", path);

        String filename = new File(path).getName();
        File file = PathUtils.getParentFile(path);
        logger.info("{} - Parent resolved to: {}", path, file.getPath());

        MediaFile parentInfo = loadMediaFile(file.getPath());
        return MediaFile.Builder.copyWithNewTitle(parentInfo, filename, PathUtils.getTitle(filename), path);
    }
}
