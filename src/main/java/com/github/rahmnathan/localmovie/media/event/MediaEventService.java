package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.MediaFileService;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.subtitle.SubtitleJobService;
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
    private final SubtitleJobService subtitleJobService;

    public void handleCreateEvent(MediaPath path) throws InvalidMediaException {
        log.info("Adding CREATE event to repository.");

        // Clean up existing events/metadata
        persistenceService.deleteAllByRelativePath(path.getRelativePath());

        MediaFile mediaFile = mediaFileService.loadMediaFile(path);
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_CREATE.getMovieEventString(), mediaFile, path.getRelativePath());
        persistenceService.saveEvent(event);

        // Queue subtitle fetch for streamable media with IMDB ID
        // This is separate from the regular media job processing because the subtitle API has limits
        if (Boolean.TRUE.equals(mediaFile.getStreamable()) && mediaFile.getMedia() != null) {
            String imdbId = mediaFile.getMedia().getImdbId();
            if (imdbId != null && !imdbId.isBlank()) {
                subtitleJobService.queueSubtitleFetch(mediaFile, imdbId);
            }
        }

        notificationHandler.sendPushNotifications(mediaFile.getMedia().getTitle(), mediaFile.getMediaFileId());
    }

    public void handleDeleteEvent(MediaPath path) {
        String relativePath = path.getRelativePath();
        if (persistenceService.existsByPath(relativePath)) {
            log.info("Removing media from database - path: {}", relativePath);
            // Delete the exact path
            persistenceService.deleteAllByRelativePath(relativePath);
            // Also delete children (path + "/") to handle directory renames - this ensures
            // all children (seasons, episodes) are deleted when a series folder is removed
            // Using "/" suffix prevents matching "ShowName Extended" when deleting "ShowName"
            persistenceService.deleteAllByRelativePathPrefix(relativePath + "/");
        }

        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_DELETE.getMovieEventString(), relativePath);
        persistenceService.saveEvent(event);
    }
}
