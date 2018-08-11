package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.omdb.boundary.OmdbMovieProvider;
import com.github.rahmnathan.omdb.data.Movie;
import com.github.rahmnathan.omdb.exception.MovieProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MovieRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MovieRepositoryMonitor.class.getName());
    private final MovieRepository movieRepository;
    private final OmdbMovieProvider movieProvider;

    public MovieRepositoryMonitor(MovieRepository movieRepository, OmdbMovieProvider movieProvider) {
        this.movieRepository = movieRepository;
        this.movieProvider = movieProvider;
    }

    @Scheduled(fixedRate = 86400000)
    public void checkForEmptyValues(){
        logger.info("Checking for null MovieInfo fields in database");

        movieRepository.findAll().forEach(mediaFile -> {
            Movie existingMovie = mediaFile.getMovie();
            if(existingMovie.hasMissingValues()){
                logger.info("Detected missing fields: {}", existingMovie.toString());

                try {
                    Movie newMovie = movieProvider.getMovie(existingMovie.getTitle());
                    Movie mergedMovie = mergeMovies(newMovie, existingMovie);
                    mediaFile.setMovie(mergedMovie);
                    movieRepository.save(mediaFile);
                } catch (MovieProviderException e){
                    logger.error("Failed to get movie from provider", e);
                }
            }
        });
    }

    private Movie mergeMovies(Movie newMovie, Movie existingMovie){
        return Movie.Builder.newInstance()
                .setGenre(newMovie.getGenre() != null && !newMovie.getGenre().equals("null") ? newMovie.getGenre() : existingMovie.getGenre())
                .setImage(newMovie.getImage() != null && !newMovie.getImage().equals("null") ? newMovie.getImage() : existingMovie.getImage())
                .setIMDBRating(newMovie.getIMDBRating() != null && !newMovie.getIMDBRating().equals("null") ? newMovie.getIMDBRating() : existingMovie.getIMDBRating())
                .setMetaRating(newMovie.getMetaRating() != null && !newMovie.getMetaRating().equals("null")? newMovie.getMetaRating() : existingMovie.getMetaRating())
                .setReleaseYear(newMovie.getReleaseYear() != null && !newMovie.getReleaseYear().equals("null") ? newMovie.getReleaseYear() : existingMovie.getReleaseYear())
                .setTitle(newMovie.getTitle() != null && !newMovie.getTitle().equals("null") ? newMovie.getTitle() : existingMovie.getTitle())
                .setActors(newMovie.getActors() != null && !newMovie.getActors().equals("null") ? newMovie.getActors() : existingMovie.getActors())
                .setPlot(newMovie.getPlot() != null && !newMovie.getPlot().equals("null") ? newMovie.getPlot() : existingMovie.getPlot())
                .build();
    }
}
