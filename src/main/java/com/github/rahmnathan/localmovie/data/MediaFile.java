package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.persistence.entity.MediaView;
import com.github.rahmnathan.omdb.data.MediaType;

import java.time.LocalDateTime;
import java.util.Set;

public interface MediaFile {
    Long getId();
    String getParentPath();
    String getPath();
    String getFileName();
    LocalDateTime getCreated();
    LocalDateTime getUpdated();
    Double getLength();
    Set<MediaView> getMediaViews();
    Media getMedia();
    String getMediaFileId();

    interface Media {
        Long getId();
        MediaType getMediaType();
        String getTitle();
        String getImdbRating();
        String getMetaRating();
        String getActors();
        String getPlot();
        String getReleaseYear();
        String getGenre();
        Integer getNumber();
        LocalDateTime getCreated();
        LocalDateTime getUpdated();
    }
}
