package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.MediaFileService;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MediaEventService {
    private final PushNotificationService notificationHandler;
    private final MediaPersistenceService persistenceService;
    private final MediaFileService mediaFileService;

    public void handleCreateEvent(MediaPath path) throws InvalidMediaException {
        log.info("Adding CREATE event to repository.");

        // Clean up existing events/metadata
        persistenceService.deleteAllByRelativePath(path.getRelativePath());

        MediaFile mediaFile = mediaFileService.loadMediaFile(path);
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_CREATE.getMovieEventString(), mediaFile, path.getRelativePath());
        persistenceService.saveEvent(event);

        notificationHandler.sendPushNotifications(mediaFile.getMedia().getTitle(), mediaFile.getParentPath());
    }

    public void handleDeleteEvent(MediaPath path) {
        String relativePath = path.getRelativePath();
        if (persistenceService.existsByPath(relativePath)) {
            log.info("Removing media from database.");
            persistenceService.deleteAllByRelativePath(relativePath);
        }

        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_DELETE.getMovieEventString(), relativePath);
        persistenceService.saveEvent(event);
    }
}
