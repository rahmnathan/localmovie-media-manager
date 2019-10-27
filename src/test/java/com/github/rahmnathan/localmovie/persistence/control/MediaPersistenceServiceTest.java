package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MediaPersistenceServiceTest {

    private final MediaPersistenceService mediaPersistenceService;
    private final MediaFileRepository mediaFileRepository;

    @Autowired
    public MediaPersistenceServiceTest(MediaPersistenceService mediaPersistenceService, MediaFileRepository mediaFileRepository) {
        this.mediaPersistenceService = mediaPersistenceService;
        this.mediaFileRepository = mediaFileRepository;
    }

    @Test
    public void getMediaFilesByParentPathTest() throws Exception {
        MediaFile mediaFile = MediaFile.Builder.newInstance()
                .setFileName("The Matrix.mp4")
                .setParentPath("Movies")
                .setPath("Movies/The Matrix.mp4")
                .setMedia(null)
                .build();

        save(mediaFile);

        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 5, MediaClient.ANDROID, MediaOrder.TITLE);

        List<MediaFile> mediaFiles = mediaPersistenceService.getMediaFilesByParentPath(mediaRequest);

        assertEquals(1, mediaFiles.size());
    }

    @Transactional
    public void save(MediaFile mediaFile) {
        mediaFileRepository.save(mediaFile);
    }
}
