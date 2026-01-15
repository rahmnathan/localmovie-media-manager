package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.TestContainersConfiguration;
import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import(TestContainersConfiguration.class)
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
    void handleCreateEventTest() throws InvalidMediaException {
        mediaEventService.handleCreateEvent(MediaPath.parse("src/test/resources/LocalMedia/Movies/300.mkv"));
        assertFalse(mediaPersistenceService.getMediaFileEvents(LocalDateTime.now().minusMinutes(1), Pageable.unpaged()).isEmpty());
    }

    @Test
    void handleDeleteEventTest() throws InvalidMediaException {
        assertTrue(mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv")).isPresent());

        mediaEventService.handleDeleteEvent(MediaPath.parse("/home/test/LocalMedia/Movies/300.mkv"));

        assertFalse(mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv")).isPresent());
    }
}
