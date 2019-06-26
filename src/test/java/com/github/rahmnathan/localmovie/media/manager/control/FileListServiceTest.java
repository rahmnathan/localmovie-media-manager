package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.media.manager.config.MediaManagerConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FileListServiceTest {

    @Test
    public void listFilesTest(){
        MediaManagerConfig config = new MediaManagerConfig();
        config.setMediaPaths(new String[]{File.separator});

        FileListService fileListService = new FileListService(config);

        Set<String> fileList = fileListService.listFiles("/test/LocalMedia/something");
        assertNotNull(fileList);
    }
}
