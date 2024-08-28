package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
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
class MediaPersistenceServiceTest {

    private final MediaPersistenceService mediaPersistenceService;

    @Autowired
    MediaPersistenceServiceTest(MediaPersistenceService mediaPersistenceService, MediaInitializer initializer) {
        this.mediaPersistenceService = mediaPersistenceService;

        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getMediaImageTest() {
        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 100, null, null, null, "movies");
        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFiles(mediaRequest);
        assertTrue(mediaFiles.size() >= 1);
    }

    @Test
    void getMediaFileByPathTest() {
        Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> resultMediaFile = mediaPersistenceService.getMediaFileByPath("Movies/300.mkv");
        assertTrue(resultMediaFile.isPresent());
    }

    @Test
    void getMediaFilesTest() {
        byte[] outputImage = mediaPersistenceService.getMediaImage("Movies/300.mkv");
        assertNotNull(outputImage);
    }

    @Test
    void getMediaFileEventsTest() {
        com.github.rahmnathan.localmovie.persistence.entity.MediaFile mediaFile = com.github.rahmnathan.localmovie.persistence.entity.MediaFile.forPath("/home/LocalMedia/some/test/path").build();
        MediaFileEvent mediaFileEvent = new MediaFileEvent("CREATE", mediaFile, "Movies");

        save(mediaFileEvent);

        List<MediaFileEvent> mediaFileEvents = mediaPersistenceService.getMediaFileEvents(LocalDateTime.now().minus(1, ChronoUnit.MINUTES), Pageable.unpaged());

        assertTrue(mediaFileEvents.size() >= 1);
    }

    @Transactional
    void save(MediaFileEvent mediaFileEvent) {
        mediaPersistenceService.saveEvent(mediaFileEvent);
    }
}
