package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.omdb.boundary.OmdbMediaProvider;
import com.github.rahmnathan.omdb.data.Media;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static com.github.rahmnathan.localmovie.control.PathUtils.getTitle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaDataServiceWebTest {
    private static final String PATH = "Series";
    private final OmdbMediaProvider mediaProvider;
    private final MediaFileRepository repository;
    private final MediaDataService dataService;

    public MediaDataServiceWebTest() {
        this.repository = mock(MediaFileRepository.class);
        this.mediaProvider = mock(OmdbMediaProvider.class);
        this.dataService = new MediaDataService(repository, mediaProvider);
    }

//    @Test
//    public void loadMediaFileTest() throws Exception {
//        when(repository.findByPath(PATH + "/Test")).thenReturn(Optional.of(MediaFile.Builder.forPath(PATH).build()));
//
//        MediaFile mediaFile = dataService.loadMediaFile(PATH + "/Test");
//
//        assertNotNull(mediaFile);
//        assertEquals(PATH, mediaFile.getParentPath());
//    }

    @Test
    public void loadNewMediaFileSeriesTest() throws Exception {
        String path = PATH + "/TestSeries";
        String title = getTitle(new File(path).getName());
        when(mediaProvider.getMovie(title)).thenReturn(Media.Builder.newInstance().setTitle(title).build());

        MediaFile mediaFile = dataService.loadNewMediaFile(path);

        assertEquals(title, mediaFile.getMedia().getTitle());
    }

    @Test
    public void loadNewMediaFileSeasonTest() throws Exception {
        String path = PATH + "/TestSeries/Season 1";
        String seriesName = "TestSeries";

        String title = getTitle(new File(path).getName());
        when(mediaProvider.getSeason(seriesName,1)).thenReturn(Media.Builder.newInstance().setTitle(title).build());
        when(mediaProvider.getMovie(seriesName)).thenReturn(Media.Builder.newInstance().setTitle(seriesName).build());

        MediaFile mediaFile = dataService.loadNewMediaFile(path);

        assertEquals(title, mediaFile.getMedia().getTitle());
    }

    @Test
    public void loadNewMediaFileEpisodeTest() throws Exception {
        String path = PATH + "/TestSeries/Season 1/Episode 1.mkv";
        String title = getTitle(new File(path).getName());
        when(mediaProvider.getEpisode("TestSeries", 1, 1)).thenReturn(Media.Builder.newInstance().setTitle(title).build());

        MediaFile mediaFile = dataService.loadNewMediaFile(path);

        assertEquals(title, mediaFile.getMedia().getTitle());
    }
}
