package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.media.MediaFileService;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

import static com.github.rahmnathan.localmovie.media.StartupMediaInitializer.ROOT_MEDIA_FOLDER;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MediaEventService {
    private final PushNotificationService notificationHandler;
    private final MediaPersistenceService persistenceService;
    private final MediaFileService mediaFileService;

    public void handleCreateEvent(File file){
        MediaFile mediaFile = mediaFileService.loadMediaFile(file);

        log.info("Adding CREATE event to repository.");
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_CREATE.getMovieEventString(), mediaFile, relativePath);
        persistenceService.saveEvent(event);

        notificationHandler.sendPushNotifications(mediaFile.getMedia().getTitle(), mediaFile.getParentPath());
    }

    public void handleDeleteEvent(File file){
        String relativePath = file.toString().split(ROOT_MEDIA_FOLDER)[1];
        if(persistenceService.existsByPath(relativePath)){
            log.info("Removing media from database.");
            persistenceService.deleteAllByRelativePath(relativePath);
        }

        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_DELETE.getMovieEventString(), relativePath);
        persistenceService.saveEvent(event);
    }
}
