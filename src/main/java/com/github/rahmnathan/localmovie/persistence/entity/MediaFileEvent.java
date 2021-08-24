package com.github.rahmnathan.localmovie.persistence.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
public class MediaFileEvent {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_file_event_sequence_generator")
    @SequenceGenerator(name="media_file_event_sequence_generator", sequenceName="MEDIA_FILE_EVENT_SEQUENCE")
    private Long id;

    private LocalDateTime timestamp;
    private String relativePath;
    private String event;

    @JoinColumn(name = "mediaFileId", referencedColumnName = "mediaFileId", unique = true)
    @OneToOne(targetEntity = MediaFile.class, cascade = CascadeType.ALL, orphanRemoval = true)
    private MediaFile mediaFile;

    public MediaFileEvent(String event, MediaFile mediaFile, String relativePath) {
        this.relativePath = relativePath;
        this.mediaFile = mediaFile;
        this.event = event;
    }

    public MediaFileEvent(String event, String relativePath) {
        this.relativePath = relativePath;
        this.event = event;
    }

    @PrePersist
    public void setTimestamp(){
        this.timestamp = LocalDateTime.now();
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
