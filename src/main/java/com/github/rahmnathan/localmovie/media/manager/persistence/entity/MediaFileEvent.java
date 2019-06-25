package com.github.rahmnathan.localmovie.media.manager.persistence.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class MediaFileEvent {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_file_event_sequence_generator")
    @SequenceGenerator(name="media_file_event_sequence_generator", sequenceName="MEDIA_FILE_EVENT_SEQUENCE")
    private Long id;

    private LocalDateTime timestamp;
    private String relativePath;
    private String event;

    @JoinColumn
    @ManyToOne(cascade = CascadeType.ALL)
    private MediaFile mediaFile;

    public MediaFileEvent() {
    }

    public MediaFileEvent(String event, MediaFile mediaFile, String relativePath) {
        this.relativePath = relativePath;
        this.mediaFile = mediaFile;
        this.event = event;
    }

    @PrePersist
    public void setTimestamp(){
        this.timestamp = LocalDateTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getEvent() {
        return event;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MediaFileEvent{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", relativePath='" + relativePath + '\'' +
                ", event='" + event + '\'' +
                ", mediaFile=" + mediaFile +
                '}';
    }
}
