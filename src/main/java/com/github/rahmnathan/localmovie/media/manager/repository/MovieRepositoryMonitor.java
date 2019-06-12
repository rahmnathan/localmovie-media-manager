package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import com.github.rahmnathan.omdb.data.Media;
import com.github.rahmnathan.omdb.exception.MediaProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.stream.StreamSupport;

@Service
public class MovieRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MovieRepositoryMonitor.class.getName());
    private final MovieRepository movieRepository;
    private final OmdbMediaProvider mediaProvider;

    public MovieRepositoryMonitor(MovieRepository movieRepository, OmdbMediaProvider mediaProvider) {
        this.movieRepository = movieRepository;
        this.mediaProvider = mediaProvider;
    }

    @Scheduled(fixedDelay = 3600000)
    public void checkForEmptyValues(){
        logger.info("Checking for null MovieInfo fields in database.");

        StreamSupport.stream(movieRepository.findAll().spliterator(), true).forEach(mediaFile -> {
            Media existingMedia = mediaFile.getMedia();
            if(existingMedia.hasMissingValues()){
                logger.info("Detected missing fields: {}", existingMedia.toString());

                try {
                    Media newMovie = mediaProvider.getMovie(existingMedia.getTitle());
                    Media mergedMovie = mergeMovies(newMovie, existingMedia);
                    mediaFile.setMedia(mergedMovie);
                    movieRepository.save(mediaFile);
                } catch (MediaProviderException e){
                    logger.error("Failed to get movie from provider", e);
                }
            }
        });
    }

    private Media mergeMovies(Media newMovie, Media existingMovie){
        return Media.Builder.newInstance()
                .setGenre(newMovie.getGenre() != null && !newMovie.getGenre().equals("null") ? newMovie.getGenre() : existingMovie.getGenre())
                .setImage(newMovie.getImage() != null && !newMovie.getImage().equals("null") ? newMovie.getImage() : existingMovie.getImage())
                .setIMDBRating(newMovie.getImdbRating() != null && !newMovie.getImdbRating().equals("null") ? newMovie.getImdbRating() : existingMovie.getImdbRating())
                .setMetaRating(newMovie.getMetaRating() != null && !newMovie.getMetaRating().equals("null")? newMovie.getMetaRating() : existingMovie.getMetaRating())
                .setReleaseYear(newMovie.getReleaseYear() != null && !newMovie.getReleaseYear().equals("null") ? newMovie.getReleaseYear() : existingMovie.getReleaseYear())
                .setTitle(newMovie.getTitle() != null && !newMovie.getTitle().equals("null") ? newMovie.getTitle() : existingMovie.getTitle())
                .setActors(newMovie.getActors() != null && !newMovie.getActors().equals("null") ? newMovie.getActors() : existingMovie.getActors())
                .setPlot(newMovie.getPlot() != null && !newMovie.getPlot().equals("null") ? newMovie.getPlot() : existingMovie.getPlot())
                .build();
    }
}
