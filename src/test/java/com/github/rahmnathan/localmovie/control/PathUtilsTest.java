package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.exception.InvalidMediaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PathUtilsTest {

    @Test
    public void getEpisodeNumberLegacyTest() throws InvalidMediaException {
        String testPath = "LocalMedia/Series/South Park/Season 01/Episode 4.mkv";

        int result = PathUtils.parseEpisodeNumber(testPath);
        assertEquals(4, result);
    }

    @Test
    public void getEpisodeNumberTest() throws InvalidMediaException {
        String testPath = "LocalMedia/Series/South Park/Season 01/S04E09.mkv";

        int result = PathUtils.parseEpisodeNumber(testPath);
        assertEquals(9, result);
    }

    @Test
    public void getEpisodeNumberInvalidNameTest() {
        String testPath = "LocalMedia/Series/South Park/Season 01/Ep 5.mkv";
        assertThrows(InvalidMediaException.class, () -> PathUtils.parseEpisodeNumber(testPath));
    }
}
