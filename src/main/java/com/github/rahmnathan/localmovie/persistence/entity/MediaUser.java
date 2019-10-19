package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(exclude="mediaView")
@Table(indexes = @Index(name = "idx_media_user_user_id", columnList = "user_id"))
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
}
