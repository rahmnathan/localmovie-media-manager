package com.github.rahmnathan.localmovie.persistence.entity;

import com.github.rahmnathan.omdb.data.MediaType;

import javax.persistence.Lob;
import java.time.LocalDateTime;
import java.util.Set;

public interface RedactedMediaFile {
    Long getId();
    String getParentPath();
    String getPath();
    String getFileName();
    LocalDateTime getCreated();
    LocalDateTime getUpdated();
    Integer getViews();
    Long getLength();
    Set<MediaView> getMediaViews();
    RedactedMedia getMedia();
    MediaFileEvent getMediaFileEvent();

    interface RedactedMedia {
        Long getId();
        MediaType getMediaType();
        String getTitle();
        String getImdbRating();
        String getMetaRating();
        String getActors();
        String getPlot();
        String getGenre();
        Integer getNumber();
        LocalDateTime getCreated();
        LocalDateTime getUpdated();
    }
}
