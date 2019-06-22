package com.github.rahmnathan.localmovie.media.manager.control.event;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.control.MediaCacheService;
import com.github.rahmnathan.localmovie.media.manager.control.MediaDataService;
import com.github.rahmnathan.localmovie.media.manager.repository.MediaEventRepository;
import com.github.rahmnathan.omdb.data.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaEventServiceTest {
    private MediaDataService dataService = mock(MediaDataService.class);
    private MediaEventService eventService;

    @BeforeEach
    public void initalize(){
        this.eventService = new MediaEventService("/fake/location", 1, dataService,
                mock(MediaEventRepository.class), mock(PushNotificationService.class), mock(MediaCacheService.class));
    }

    @Test
    public void directoryMonitorTest() throws Exception {

        eventService.handleDeleteEvent("/my/test/LocalMedia/path");

        MediaFile mediaFile = MediaFile.Builder.newInstance().setPath("").setMedia(Media.Builder.newInstance().setTitle("").build()).build();
        when(dataService.loadMediaFile(any())).thenReturn(mediaFile);
        when(dataService.saveMediaFile(any())).thenReturn(mediaFile);
        eventService.handleCreateEvent("Movies/Test.mp4", Paths.get("/my/test/LocalMedia/path"), Collections.emptySet());
    }
}
