package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.persistence.entity.RedactedMediaFile;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MediaPersistenceServiceTest {

    private final MediaPersistenceService mediaPersistenceService;

    @Autowired
    public MediaPersistenceServiceTest(MediaPersistenceService mediaPersistenceService) {
        this.mediaPersistenceService = mediaPersistenceService;
    }

    @Test
    public void getMediaFilesByParentPathTest() {
        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 5, MediaClient.ANDROID, MediaOrder.TITLE);
        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFilesByParentPath(mediaRequest);
        assertTrue(mediaFiles.size() > 1);
    }

    @Test
    public void getMediaImageTest() {
        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 5, MediaClient.ANDROID, MediaOrder.TITLE);
        List<RedactedMediaFile> mediaFiles = mediaPersistenceService.getMediaFilesByParentPathNoPoster(mediaRequest);
        assertTrue(mediaFiles.size() >= 1);
    }

    @Test
    public void getMediaFileByPathTest() {
        Optional<MediaFile> resultMediaFile = mediaPersistenceService.getMediaFileByPath("Movies/300.mkv");
        assertTrue(resultMediaFile.isPresent());
    }

    @Test
    public void getMediaFilesByParentPathNoPosterTest() {
        Media media = new Media();
        byte[] image = "MyTestImage".getBytes();
        media.setImage(image);

        MediaFile mediaFile = MediaFile.Builder.newInstance()
                .setFileName("American Sniper.mp4")
                .setParentPath("Movies")
                .setPath("Movies/American Sniper.mp4")
                .setMedia(media)
                .build();

        save(mediaFile);

        byte[] outputImage = mediaPersistenceService.getMediaImage("Movies/American Sniper.mp4");

        assertArrayEquals(image, outputImage);
    }

    @Test
    public void addViewTest() {
        mediaPersistenceService.addView("/home/user/LocalMedia/Movies/300.mkv", 400L);

        Optional<MediaFile> resultMediaFile = mediaPersistenceService.getMediaFileByPath("Movies/300.mkv");

        assertTrue(resultMediaFile.isPresent());
        assertTrue(resultMediaFile.get().getMediaViews().size() >= 1);
    }

    @Test
    public void getMediaFileEventsTest() {
        MediaFileEvent mediaFileEvent = new MediaFileEvent("CREATE", null, "Movies");

        save(mediaFileEvent);

        List<MediaFileEvent> mediaFileEvents = mediaPersistenceService.getMediaFileEvents(LocalDateTime.now().minus(1, ChronoUnit.MINUTES));

        assertTrue(mediaFileEvents.size() >= 1);
    }

    @Transactional
    public void save(MediaFileEvent mediaFileEvent) {
        mediaPersistenceService.saveEvent(mediaFileEvent);
    }

    @Transactional
    public void save(MediaFile mediaFile) {
        mediaPersistenceService.saveMediaFile(mediaFile);
    }
}
