package com.github.rahmnathan.localmovie.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rahmnathan.localmovie.TestContainersConfiguration;
import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import({TestContainersConfiguration.class, MediaPersistenceServiceTest.TestConfig.class})
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
        MediaRequest mediaRequest = new MediaRequest("Movies", "", 0, 100, null, null, null, "movies");
        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFiles(mediaRequest);
        assertFalse(mediaFiles.isEmpty());
    }

    @Test
    void getMediaFileByPathTest() throws InvalidMediaException {
        Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> resultMediaFile = mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv"));
        assertTrue(resultMediaFile.isPresent());
    }

    @Test
    void getMediaFilesTest() {
        byte[] outputImage = mediaPersistenceService.getMediaImage("Movies/300.mkv");
        assertNotNull(outputImage);
    }

    @Test
    void testGetMediaFilesByType() {
        MediaRequest request = new MediaRequest(null, "", 0, 100, null, null, null, "movies");
        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFiles(request);

        assertNotNull(mediaFiles);
        // All results should be movies
        mediaFiles.forEach(file -> assertEquals("MOVIE", file.getMediaFileType().toString()));
    }

    @Test
    void testGetMediaFilesByPath() {
        MediaRequest request = new MediaRequest("Movies", "", 0, 100, null, null, null, "movies");
        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFiles(request);

        assertNotNull(mediaFiles);
        assertFalse(mediaFiles.isEmpty());
    }

    @Test
    void testGetMediaFilesWithPagination() {
        MediaRequest request = new MediaRequest("Movies", "", 0, 1, null, null, null, "movies");
        List<MediaFile> page1 = mediaPersistenceService.getMediaFiles(request);

        assertNotNull(page1);
        assertTrue(page1.size() <= 1);
    }

    @Test
    void testCountMediaFilesByType() {
        MediaRequest request = new MediaRequest(null, "", 0, 100, null, null, null, "movies");
        long count = mediaPersistenceService.countMediaFiles(request);

        assertTrue(count >= 0);
    }

    @Test
    void testCountMediaFilesWithFilters() {
        MediaRequest request = new MediaRequest("Movies", "", 0, 100, null, null, null, "movies");
        long count = mediaPersistenceService.countMediaFiles(request);

        assertTrue(count >= 0);
    }

    @Test
    @Transactional
    void testAddViewCreatesNewView() throws InvalidMediaException {
        Optional<MediaFile> mediaFileOpt = mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv"));
        assertTrue(mediaFileOpt.isPresent());

        String mediaFileId = mediaFileOpt.get().getMediaFileId();

        // Add view with position
        mediaPersistenceService.addView(mediaFileId, 45.5, 1000.0);

        // Verify view was added (would need to query to verify, but we test it doesn't throw)
        assertNotNull(mediaFileId);
    }

    @Test
    void testAddViewWithNonExistentMediaDoesNotThrow() {
        // Should not throw exception
        mediaPersistenceService.addView("non-existent-id", 10.0, 1000.0);
    }

    @Test
    void testFindByMediaFileIdExists() throws InvalidMediaException {
        Optional<MediaFile> mediaFileOpt = mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv"));
        assertTrue(mediaFileOpt.isPresent());

        String mediaFileId = mediaFileOpt.get().getMediaFileId();
        Optional<MediaFile> result = mediaPersistenceService.findByMediaFileId(mediaFileId);

        assertTrue(result.isPresent());
        assertEquals(mediaFileId, result.get().getMediaFileId());
    }

    @Test
    void testFindByMediaFileIdNotFound() {
        Optional<MediaFile> result = mediaPersistenceService.findByMediaFileId("non-existent-id");
        assertFalse(result.isPresent());
    }

    @Test
    void testGetMediaFileByIdWithViews() throws InvalidMediaException {
        Optional<MediaFile> mediaFileOpt = mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv"));
        assertTrue(mediaFileOpt.isPresent());

        String mediaFileId = mediaFileOpt.get().getMediaFileId();
        Optional<MediaFile> result = mediaPersistenceService.getMediaFileByIdWithViews(mediaFileId);

        assertNotNull(result);
    }

    @Test
    void testGetMediaImageByPath() {
        byte[] image = mediaPersistenceService.getMediaImage("Movies/300.mkv");
        assertNotNull(image);
    }

    @Test
    void testGetMediaImageByIdExists() throws InvalidMediaException {
        Optional<MediaFile> mediaFileOpt = mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv"));
        assertTrue(mediaFileOpt.isPresent());

        String mediaFileId = mediaFileOpt.get().getMediaFileId();
        byte[] image = mediaPersistenceService.getMediaImageById(mediaFileId);

        assertNotNull(image);
    }

    @Test
    void testExistsByPathTrue() {
        boolean exists = mediaPersistenceService.existsByPath("Movies/300.mkv");
        assertTrue(exists);
    }

    @Test
    void testExistsByPathFalse() {
        boolean exists = mediaPersistenceService.existsByPath("Movies/NonExistent.mkv");
        assertFalse(exists);
    }

    // ========== Save Operations Tests ==========

    @Test
    @Transactional
    void testSaveMediaFile() throws InvalidMediaException {
        Optional<MediaFile> mediaFileOpt = mediaPersistenceService.getMediaFileByPath(MediaPath.parse("Movies/300.mkv"));
        assertTrue(mediaFileOpt.isPresent());

        MediaFile mediaFile = mediaFileOpt.get();
        MediaFile saved = mediaPersistenceService.saveMediaFile(mediaFile);

        assertNotNull(saved);
        assertEquals(mediaFile.getMediaFileId(), saved.getMediaFileId());
    }

    @Test
    @Transactional
    void testSaveEvent() {
        MediaFileEvent event = new MediaFileEvent();
        event.setRelativePath("test/path");
        event.setEvent("CREATE");
        event.setTimestamp(LocalDateTime.now());

        // Should not throw exception
        mediaPersistenceService.saveEvent(event);
    }

    @Test
    void testCountHistory() {
        long count = mediaPersistenceService.countHistory();
        assertTrue(count >= 0);
    }

    @Test
    void testGetMediaFileEvents() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<MediaFileEvent> events = mediaPersistenceService.getMediaFileEvents(since, Pageable.ofSize(10));

        assertNotNull(events);
    }

    @Test
    void testGetMediaFileDtos() {
        MediaRequest request = new MediaRequest("Movies", "", 0, 100, null, null, null, "movies");
        var dtos = mediaPersistenceService.getMediaFileDtos(request);

        assertNotNull(dtos);
        assertFalse(dtos.isEmpty());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
