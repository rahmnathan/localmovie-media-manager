package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.omdb.data.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MediaPathTest {

    @Test
    void moviePath_Absolute() throws InvalidMediaException {
        String testPath = "/media/nathan/LocalMedia/Movies/John Wick.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(testPath, mediaPath.getAbsolutePath());
        assertEquals("Movies/John Wick.mkv", mediaPath.getRelativePath());
        assertEquals(MediaType.MOVIE, mediaPath.getMediaType());
    }

    @Test
    void moviePath_Relative() throws InvalidMediaException {
        String testPath = "Movies/John Wick.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(testPath, mediaPath.getRelativePath());
        assertEquals(MediaType.MOVIE, mediaPath.getMediaType());
    }

    @Test
    void seriesPath_Absolute() throws InvalidMediaException {
        String testPath = "/media/nathan/LocalMedia/Series/Breaking Bad";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(testPath, mediaPath.getAbsolutePath());
        assertEquals("Series/Breaking Bad", mediaPath.getRelativePath());
        assertEquals(MediaType.SERIES, mediaPath.getMediaType());
    }

    @Test
    void seriesPath_Relative() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(testPath, mediaPath.getRelativePath());
        assertEquals(MediaType.SERIES, mediaPath.getMediaType());
    }

    @Test
    void seasonPath_Absolute() throws InvalidMediaException {
        String testPath = "/media/nathan/LocalMedia/Series/Breaking Bad/Season 1";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(testPath, mediaPath.getAbsolutePath());
        assertEquals("Series/Breaking Bad/Season 1", mediaPath.getRelativePath());
        assertEquals("Series/Breaking Bad", mediaPath.getSeriesPath().getRelativePath());
        assertEquals("Breaking Bad", mediaPath.getSeriesPath().getTitle());
        assertEquals(MediaType.SEASON, mediaPath.getMediaType());
    }

    @Test
    void seasonPath_Relative() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(testPath, mediaPath.getRelativePath());
        assertEquals(MediaType.SERIES, mediaPath.getMediaType());
    }

    @Test
    void getEpisodeNumberLegacyTest() throws InvalidMediaException {
        String testPath = "Series/South Park/Season 01/Episode 4.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(4, mediaPath.getEpisodeNumber());
    }

    @Test
    void getEpisodeNumberTest() throws InvalidMediaException {
        String testPath = "Series/South Park/Season 01/S04E09.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(9, mediaPath.getEpisodeNumber());
    }

    @Test
    void getEpisodeNumberInvalidNameTest() {
        String testPath = "Series/South Park/Season 01/Ep 5.mkv";

        assertThrows(InvalidMediaException.class, () -> MediaPath.parse(testPath));
    }

    @Test
    void getMediaType_Movie() throws InvalidMediaException {
        String testPath = "Movies/John Wick.mp4";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(MediaType.MOVIE, mediaPath.getMediaType());
    }

    @Test
    void getMediaType_Movie_Nested() throws InvalidMediaException {
        String testPath = "Movies/John Wick/John Wick.mp4";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(MediaType.MOVIE, mediaPath.getMediaType());
    }

    @Test
    void getMediaType_Series() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(MediaType.SERIES, mediaPath.getMediaType());
    }

    @Test
    void getMediaType_Season() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad/Season 1";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(MediaType.SEASON, mediaPath.getMediaType());
    }

    @Test
    void getMediaType_Episode() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad/Season 1/Episode 1.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(MediaType.EPISODE, mediaPath.getMediaType());
    }

    @Test
    void getMediaType_Episode_Nested() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad/Season 1/Episode 1/Episode 1.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals(MediaType.EPISODE, mediaPath.getMediaType());
    }

    @Test
    void testGetSeriesPath() throws InvalidMediaException {
        String testPath = "Series/Breaking Bad/Season 1/Episode 1.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals("Series/Breaking Bad", mediaPath.getSeriesPath().getRelativePath());
    }

    @Test
    void testGetSeriesPath_Relative() throws InvalidMediaException {
        String testPath = "/media/nathan/LocalMedia/Series/Breaking Bad/Season 1/Episode 1.mkv";
        MediaPath mediaPath = MediaPath.parse(testPath);

        assertEquals("Series/Breaking Bad", mediaPath.getSeriesPath().getRelativePath());
    }
}
