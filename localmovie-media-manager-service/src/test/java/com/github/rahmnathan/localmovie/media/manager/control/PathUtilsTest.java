package com.github.rahmnathan.localmovie.media.manager.control;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathUtilsTest {

    @Test
    public void testGetParentFile(){
        String directory = "Series/Westworld";
        assertEquals(System.getProperty("user.dir") + File.separator + directory, PathUtils.getParentFile(directory + "/Season 1").getAbsolutePath());
    }

    @Test
    public void getTitle(){
        String fileName = "fakeFileName";
        assertEquals(fileName, PathUtils.getTitle(fileName + ".mp4"));
    }
}
