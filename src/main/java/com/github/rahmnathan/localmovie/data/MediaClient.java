package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum MediaClient {
    ANDROID(MediaPersistenceService::getMediaFilesByParentPath),
    WEBAPP(MediaPersistenceService::getMediaFilesByParentPathNoPoster);

    private final MediaProvider mediaProvider;

    @FunctionalInterface
    public interface MediaProvider {
        List<? extends MediaFile> getMedia(MediaPersistenceService persistenceService, MediaRequest mediaRequest);
    }
}
