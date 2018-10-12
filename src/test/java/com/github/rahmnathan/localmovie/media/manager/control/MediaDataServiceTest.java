package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.repository.MovieRepository;
import com.github.rahmnathan.omdb.boundary.OmdbMovieProvider;
import com.github.rahmnathan.omdb.data.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class MediaDataServiceTest {
    private static final String SERIES_FILE_PATH = "Series/Westworld/Season 1/Episode 1.mkv";
    private static final String SERIES_FILE_TITLE = "Episode 1";
    private static final String MOVIE_FILE_PATH = "Movies/TestMovie.mkv";
    private static final String MOVIE_FILE_TITLE = "TestMovie";
    private MediaDataService dataService;

    @Autowired
    private MovieRepository movieRepository;
    private OmdbMovieProvider movieProvider;

    @BeforeEach
    public void initialize(){
        this.movieProvider = mock(OmdbMovieProvider.class);

        this.dataService = new MediaDataService(movieRepository, movieProvider);
    }

    @Test
    public void loadExistingMediaFileTest(){
        MediaFile mediaFile = MediaFile.Builder.newInstance().setPath(SERIES_FILE_PATH).build();
        movieRepository.save(mediaFile);

        MediaFile responseFile = dataService.loadMediaFile(SERIES_FILE_PATH);
        assertEquals(mediaFile.getPath(), responseFile.getPath());
    }

    @Test
    public void loadMissingMediaFileTest() throws Exception {
        Movie movie = Movie.Builder.newInstance().setTitle(MOVIE_FILE_TITLE).build();
        when(movieProvider.getMovie(MOVIE_FILE_TITLE)).thenReturn(movie);


        MediaFile mediaFile = dataService.loadMediaFile(MOVIE_FILE_PATH);

        assertEquals(movie.getTitle(), mediaFile.getMovie().getTitle());
    }

    @Test
    public void loadParentFileTest() throws Exception {
        Movie movie = Movie.Builder.newInstance().setTitle(SERIES_FILE_TITLE).build();
        when(movieProvider.getMovie(SERIES_FILE_TITLE)).thenReturn(movie);


        MediaFile mediaFile = dataService.loadMediaFile(SERIES_FILE_PATH);

        assertEquals(movie.getTitle(), mediaFile.getMovie().getTitle());
    }
}
