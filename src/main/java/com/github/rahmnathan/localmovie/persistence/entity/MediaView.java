package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_media_view", columnList = "media_file_id,media_user_id", unique = true))
@EqualsAndHashCode(exclude="mediaFile")
public class MediaView {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_view_sequence_generator")
    @SequenceGenerator(name="media_view_sequence_generator", sequenceName="MEDIA_VIEW_SEQUENCE")
    private Long id;

    private Double position;

    @ManyToOne
    private MediaFile mediaFile;
    @ManyToOne
    private MediaUser mediaUser;

    private LocalDateTime created;
    private LocalDateTime updated;

    public MediaView(MediaFile mediaFile, MediaUser mediaUser, Double position) {
        this.mediaFile = mediaFile;
        this.mediaUser = mediaUser;
        this.position = position;
    }

    @PrePersist
    public void setCreated(){
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void setUpdated(){
        updated = LocalDateTime.now();
    }

    @JsonIgnore
    public MediaFile getMediaFile(){
        return mediaFile;
    }
}
