package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.media.manager.config.MediaManagerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaDirectoryMonitorTest {
    private MediaDirectoryMonitor mediaDirectoryMonitor;

    @BeforeEach
    public void initialize(){
        FileListService fileListService = mock(FileListService.class);
        when(fileListService.listFiles(any())).thenReturn(Collections.emptySet());

        MediaManagerConfig config = new MediaManagerConfig();
        config.setMediaPaths(new String[]{"/my/test/LocalMedia/"});

        this.mediaDirectoryMonitor = new MediaDirectoryMonitor(Collections.emptySet(), config, fileListService,
                mock(MediaCacheService.class), mock(MediaDataService.class));
    }

    @Test
    public void initializeFileListTest(){
        mediaDirectoryMonitor.initializeFileList();
    }
}
