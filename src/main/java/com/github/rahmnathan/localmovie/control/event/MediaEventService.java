package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.localmovie.control.MediaFileService;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

import static com.github.rahmnathan.localmovie.control.MediaDatabaseInitializer.ROOT_MEDIA_FOLDER;

@Service
@Transactional
@AllArgsConstructor
public class MediaEventService {
    private final Logger logger = LoggerFactory.getLogger(MediaEventService.class);
    private final PushNotificationService notificationHandler;
    private final MediaPersistenceService persistenceService;
    private final MediaFileService mediaFileService;

    public void handleCreateEvent(File file){
        MediaFile mediaFile = mediaFileService.loadMediaFile(file);

        logger.info("Adding CREATE event to repository.");
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_CREATE.getMovieEventString(), mediaFile, relativePath);
        mediaFile.setMediaFileEvent(event);
        persistenceService.saveEvent(event, mediaFile);

        notificationHandler.sendPushNotifications(mediaFile.getMedia().getTitle(), mediaFile.getParentPath());
    }

    public void handleDeleteEvent(File file){
        String relativePath = file.toString().split(ROOT_MEDIA_FOLDER)[1];
        if(persistenceService.existsByPath(relativePath)){
            logger.info("Removing media from database.");
            persistenceService.deleteAllByRelativePath(relativePath);
        }

        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_DELETE.getMovieEventString(), relativePath);
        persistenceService.saveEvent(event);
    }
}
