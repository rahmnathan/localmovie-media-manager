package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.repository.MediaEventRepository;
import com.github.rahmnathan.omdb.data.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaFileEventManagerTest {
    private MediaDataService dataService = mock(MediaDataService.class);
    private MediaFileEventManager eventManager;

    @BeforeEach
    public void initalize(){
        this.eventManager = new MediaFileEventManager("/fake/location", 1, dataService,
                mock(MediaEventRepository.class), mock(PushNotificationHandler.class), mock(MediaCacheService.class));
    }

    @Test
    public void directoryMonitorTest(){
        WatchEvent watchEvent = mock(WatchEvent.class);
        when(watchEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_DELETE);

        eventManager.directoryModified(watchEvent, Paths.get("/my/test/LocalMedia/path"));

        when(watchEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_CREATE);
        MediaFile mediaFile = MediaFile.Builder.newInstance().setMedia(Media.Builder.newInstance().build()).build();
        when(dataService.loadMediaFile(any())).thenReturn(mediaFile);
        eventManager.directoryModified(watchEvent, Paths.get("/my/test/LocalMedia/path"));
    }
}
