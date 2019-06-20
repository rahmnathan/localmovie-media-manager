package com.github.rahmnathan.localmovie.media.manager.control;

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
        this.mediaDirectoryMonitor = new MediaDirectoryMonitor(Collections.emptySet(), new String[]{"/my/test/LocalMedia/"}, fileListService,
                mock(MediaCacheService.class), mock(MediaDataService.class));
    }

    @Test
    public void initializeFileListTest(){
        mediaDirectoryMonitor.initializeFileList();
    }
}
