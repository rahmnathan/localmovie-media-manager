package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MediaFileServiceTest {
    private final MediaFileService mediaFileService;

    @Autowired
    public MediaFileServiceTest(MediaFileService mediaFileService, StartupMediaInitializer initializer) {
        this.mediaFileService = mediaFileService;

        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void loadMovieTest() {
        MediaFile mediaFile = mediaFileService.loadMediaFile(new File("/home/test/LocalMedia/Movies/300.mkv"));
        assertNotNull(mediaFile);
    }

    @Test
    public void loadSeasonTest() {
        MediaFile mediaFile = mediaFileService.loadMediaFile(new File("/home/test/LocalMedia/Series/Game of Thrones/Season 1"));
        assertNotNull(mediaFile);
    }

    @Test
    public void loadEpisodeTest() {
        MediaFile mediaFile = mediaFileService.loadMediaFile(new File("/home/test/LocalMedia/Series/Game of Thrones/Season 1/Episode 1.mkv"));
        assertNotNull(mediaFile);
    }
}
