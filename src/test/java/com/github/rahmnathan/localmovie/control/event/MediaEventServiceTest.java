package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MediaEventServiceTest {
    private final MediaPersistenceService mediaPersistenceService;
    private final MediaEventService mediaEventService;

    @Autowired
    public MediaEventServiceTest(MediaPersistenceService mediaPersistenceService, MediaEventService mediaEventService) {
        this.mediaPersistenceService = mediaPersistenceService;
        this.mediaEventService = mediaEventService;
    }

    @Test
    public void handleCreateEventTest() {
        mediaEventService.handleCreateEvent(new File("src/test/resources/LocalMedia/Movies/300.mkv"), new HashSet<>());
        assertTrue(mediaPersistenceService.getMediaFileEvents(LocalDateTime.now().minus(1, ChronoUnit.MINUTES)).size() >= 1);
    }

    @Test
    public void handleDeleteEventTest() {
        assertTrue(mediaPersistenceService.getMediaFileByPath("Movies/300.mkv").isPresent());

        mediaEventService.handleDeleteEvent(new File("/home/test/LocalMedia/Movies/300.mkv"));

        assertFalse(mediaPersistenceService.getMediaFileByPath("Movies/300.mkv").isPresent());
    }
}
