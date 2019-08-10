package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
//@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"media_file_id", "media_user_id)"}))
public class MediaView {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_view_sequence_generator")
    @SequenceGenerator(name="media_view_sequence_generator", sequenceName="MEDIA_VIEW_SEQUENCE")
    private Long id;

    private Long position;

    @ManyToOne
    private MediaFile mediaFile;
    @ManyToOne(cascade = CascadeType.ALL)
    private MediaUser mediaUser;

    private LocalDateTime created;
    private LocalDateTime updated;

    public MediaView(MediaFile mediaFile, MediaUser mediaUser, Long position) {
        this.mediaFile = mediaFile;
        this.mediaUser = mediaUser;
        this.position = position;
    }

    public MediaView() {

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

    public Long getId() {
        return id;
    }

    public MediaUser getMediaUser(){
        return mediaUser;
    }

    @JsonIgnore
    public MediaFile getMediaFile(){
        return mediaFile;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }
}
