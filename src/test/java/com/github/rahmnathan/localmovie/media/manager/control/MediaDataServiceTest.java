package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.repository.MediaRepository;
import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import com.github.rahmnathan.omdb.data.Media;
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
    private static final String SERIES_FILE_PATH = "Series/Game of Thrones/Season 13/Episode 5.mkv";
    private static final String SERIES_FILE_TITLE = "Episode 1";
    private static final String SERIES_TITLE = "Game of Thrones";
    private static final String MOVIE_FILE_PATH = "Movies/TestMedia.mkv";
    private static final String MOVIE_FILE_TITLE = "TestMedia";
    private MediaDataService dataService;

    @Autowired
    private MediaRepository mediaRepository;
    private OmdbMediaProvider movieProvider;

    @BeforeEach
    public void initialize(){
        this.movieProvider = mock(OmdbMediaProvider.class);

        this.dataService = new MediaDataService(mediaRepository, movieProvider);
    }

    @Test
    public void loadExistingMediaFileTest() throws Exception {
        MediaFile mediaFile = MediaFile.Builder.newInstance().setPath(SERIES_FILE_PATH).build();
        mediaRepository.save(mediaFile);

        MediaFile responseFile = dataService.loadMediaFile(SERIES_FILE_PATH);
        assertEquals(mediaFile.getPath(), responseFile.getPath());
    }

    @Test
    public void loadMissingMediaFileTest() throws Exception {
        Media movie = Media.Builder.newInstance().setTitle(MOVIE_FILE_TITLE).build();
        when(movieProvider.getMovie(MOVIE_FILE_TITLE)).thenReturn(movie);


        MediaFile mediaFile = dataService.loadMediaFile(MOVIE_FILE_PATH);

        assertEquals(movie.getTitle(), mediaFile.getMedia().getTitle());
    }

    @Test
    public void loadParentFileTest() throws Exception {
        Media movie = Media.Builder.newInstance().setTitle(SERIES_FILE_TITLE).build();
        when(movieProvider.getEpisode(SERIES_TITLE, 13, 5)).thenReturn(movie);


        MediaFile mediaFile = dataService.loadMediaFile(SERIES_FILE_PATH);

        assertEquals(movie.getTitle(), mediaFile.getMedia().getTitle());
    }
}
