package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.web.data.MediaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MediaDataServiceWeb {
    private final Logger logger = LoggerFactory.getLogger(MediaDataServiceWeb.class);
    private final MediaPersistenceService persistenceService;

    public MediaDataServiceWeb(MediaPersistenceService cacheService) {
        this.persistenceService = cacheService;
    }

    public long loadMediaListLength(String directoryPath){
        return persistenceService.countMediaFiles(directoryPath);
    }

    public boolean existsByPath(String filePath) {
        return persistenceService.existsByPath(filePath);
    }

    public byte[] loadMediaPoster(String path){
        return persistenceService.getMediaImage(path);
    }

    public List<MediaFile> loadMediaFileList(MediaRequest mediaRequest) {
        List<MediaFile> mediaFiles = persistenceService.getMediaFilesByParentPath(mediaRequest);

        if (mediaRequest.getClient() == MediaClient.WEBAPP) {
            logger.info("Removing images for webapp");
            mediaFiles.forEach(mediaFile -> mediaFile.getMedia().removePoster());
        }

        return mediaFiles;
    }

    public List<MediaFileEvent> getMediaFileEvents(LocalDateTime dateTime){
        return persistenceService.getMediaFileEvents(dateTime);
    }
}
