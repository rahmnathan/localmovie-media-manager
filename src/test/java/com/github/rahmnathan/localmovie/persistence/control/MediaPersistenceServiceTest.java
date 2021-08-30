package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.control.StartupMediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.data.MediaFile;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MediaPersistenceServiceTest {

    private final MediaPersistenceService mediaPersistenceService;

    @Autowired
    public MediaPersistenceServiceTest(MediaPersistenceService mediaPersistenceService, StartupMediaInitializer mediaInitializer) {
        this.mediaPersistenceService = mediaPersistenceService;

        mediaInitializer.initializeFileListSynchronous();
    }

    @Test
    public void getMediaFilesByParentPathTest() {
        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 5, MediaClient.ANDROID, MediaOrder.TITLE);
        List<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> mediaFiles = mediaPersistenceService.getMediaFilesByParentPath(mediaRequest);
        assertTrue(mediaFiles.size() > 0);
    }

    @Test
    public void getMediaImageTest() {
        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 5, MediaClient.ANDROID, MediaOrder.TITLE);
        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFilesByParentPathNoPoster(mediaRequest);
        assertTrue(mediaFiles.size() >= 1);
    }

    @Test
    public void getMediaFileByPathTest() {
        Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> resultMediaFile = mediaPersistenceService.getMediaFileByPath("Movies/300.mkv");
        assertTrue(resultMediaFile.isPresent());
    }

    @Test
    public void getMediaFilesByParentPathNoPosterTest() {
        byte[] outputImage = mediaPersistenceService.getMediaImage("Movies/300.mkv");
        assertNotNull(outputImage);
    }

    @Test
    public void getMediaFileEventsTest() {
        com.github.rahmnathan.localmovie.persistence.entity.MediaFile mediaFile = com.github.rahmnathan.localmovie.persistence.entity.MediaFile.Builder.forPath("/home/LocalMedia/some/test/path").build();
        MediaFileEvent mediaFileEvent = new MediaFileEvent("CREATE", mediaFile, "Movies");

        save(mediaFileEvent);

        List<MediaFileEvent> mediaFileEvents = mediaPersistenceService.getMediaFileEvents(LocalDateTime.now().minus(1, ChronoUnit.MINUTES), Pageable.unpaged());

        assertTrue(mediaFileEvents.size() >= 1);
    }

    @Transactional
    public void save(MediaFileEvent mediaFileEvent) {
        mediaPersistenceService.saveEvent(mediaFileEvent);
    }
}
