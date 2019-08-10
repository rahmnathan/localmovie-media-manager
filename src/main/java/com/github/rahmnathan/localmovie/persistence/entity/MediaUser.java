package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
public class MediaUser {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_user_sequence_generator")
    @SequenceGenerator(name="media_user_sequence_generator", sequenceName="MEDIA_USER_SEQUENCE")
    private Long id;

    private String userId;
    private LocalDateTime created;
    private LocalDateTime updated;

    @OneToMany(mappedBy = "mediaUser")
    private Set<MediaView> mediaView = new HashSet<>();

    public MediaUser(String userId){
        this.userId = userId;
    }

    public MediaUser(){

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
    public Collection<MediaView> getMediaView() {
        return mediaView;
    }

    public void addMediaView(MediaView mediaView) {
        this.mediaView.add(mediaView);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }
}
