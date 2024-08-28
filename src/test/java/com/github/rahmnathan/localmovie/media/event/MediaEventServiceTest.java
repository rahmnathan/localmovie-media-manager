package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class MediaEventServiceTest {
    private final MediaPersistenceService mediaPersistenceService;
    private final MediaEventService mediaEventService;

    @Autowired
    MediaEventServiceTest(MediaPersistenceService mediaPersistenceService, MediaEventService mediaEventService, MediaInitializer initializer) {
        this.mediaPersistenceService = mediaPersistenceService;
        this.mediaEventService = mediaEventService;

        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void handleCreateEventTest() {
        mediaEventService.handleCreateEvent(new File("src/test/resources/LocalMedia/Movies/300.mkv"));
        assertFalse(mediaPersistenceService.getMediaFileEvents(LocalDateTime.now().minus(1, ChronoUnit.MINUTES), Pageable.unpaged()).isEmpty());
    }

    @Test
    void handleDeleteEventTest() {
        assertTrue(mediaPersistenceService.getMediaFileByPath("Movies/300.mkv").isPresent());

        mediaEventService.handleDeleteEvent(new File("/home/test/LocalMedia/Movies/300.mkv"));

        assertFalse(mediaPersistenceService.getMediaFileByPath("Movies/300.mkv").isPresent());
    }
}
