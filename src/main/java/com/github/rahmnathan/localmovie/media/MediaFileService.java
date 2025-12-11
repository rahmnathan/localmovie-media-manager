package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.ffprobe.VideoMetadataService;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@AllArgsConstructor
public class MediaFileService {
    private final MediaPersistenceService persistenceService;
    private final MediaService mediaService;
    private final VideoMetadataService videoMetadataService;

    @Transactional
    public MediaFile loadMediaFile(MediaPath path) {
        Optional<MediaFile> mediaFile = persistenceService.getMediaFileByPath(path);
        if (mediaFile.isPresent()) {
            log.info("Getting from database - {}", path);
            return mediaFile.get();
        }

        return loadNewMediaFile(path);
    }

    private MediaFile loadNewMediaFile(MediaPath path) {
        MediaFile mediaFile = MediaFile.forPath(path.getAbsolutePath())
                .media(mediaService.loadMedia(path))
                .mediaFileId(UUID.randomUUID().toString())
                .metadataAnalyzed(false)
                .build();

        MediaFile saved = persistenceService.saveMediaFile(mediaFile);

        // Trigger async metadata analysis
        CompletableFuture.runAsync(() -> videoMetadataService.analyzeAndUpdate(saved));

        return saved;
    }
}
