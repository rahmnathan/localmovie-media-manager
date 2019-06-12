package com.github.rahmnathan.localmovie.media.manager.control;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.rahmnathan.localmovie.media.manager.control.PathUtils.getEpisodeNumber;
import static com.github.rahmnathan.localmovie.media.manager.control.PathUtils.getSeasonNumber;
import static org.junit.jupiter.api.Assertions.*;

public class PathUtilsTest {

    @Test
    public void testGetParentFile(){
        String directory = "Series/Westworld";
        assertEquals(System.getProperty("user.dir") + File.separator + directory, PathUtils.getParentFile(directory + "/Season 1").getAbsolutePath());
    }

    @Test
    public void isTopLevelTest(){
        assertTrue(PathUtils.isTopLevel("Series/Westworld"));
        assertTrue(PathUtils.isTopLevel("Movies/400.mkv"));
        assertFalse(PathUtils.isTopLevel("Series/Westworld/Season 13/Episode 43.mkv"));
    }

    @Test
    public void getEpisodeNumberTest(){
        String input = "Episode 11.mp4";
        assertEquals(11, getEpisodeNumber(input));
    }

    @Test
    public void getSeasonNumberTest(){
        String path = "Series/Westworld/Season 43/Episode 13.mp4";
        assertEquals(43, getSeasonNumber(path));
    }

    @Test
    public void getTitle(){
        String fileName = "fakeFileName";
        assertEquals(fileName, PathUtils.getTitle(fileName + ".mp4"));
    }
}
