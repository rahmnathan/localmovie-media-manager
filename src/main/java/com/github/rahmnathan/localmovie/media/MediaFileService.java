package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class MediaFileService {
    private final MediaPersistenceService persistenceService;
    private final MediaService mediaService;

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
        MediaFile mediaFile = MediaFile.forPath(path)
                .media(mediaService.loadMedia(path))
                .mediaFileId(UUID.randomUUID().toString())
                .build();

        synchronized (persistenceService) {
            MediaPath parentPath = path.getParentPath();
            if (parentPath != null) {
                MediaFile parent = loadMediaFile(parentPath);
                mediaFile.setParent(parent);
            }

            return persistenceService.saveMediaFile(mediaFile);
        }
    }
}
