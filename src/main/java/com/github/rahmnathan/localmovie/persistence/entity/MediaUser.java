package com.github.rahmnathan.localmovie.persistence.entity;

import lombok.*;
import org.hibernate.Hibernate;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_media_user_user_id", columnList = "userId"))
public class MediaUser {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_user_sequence_generator")
    @SequenceGenerator(name="media_user_sequence_generator", sequenceName="MEDIA_USER_SEQUENCE")
    private Long id;

    private String userId;
    private LocalDateTime created;
    private LocalDateTime updated;

    @OneToMany(mappedBy = "mediaUser")
    @ToString.Exclude
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

    public Collection<MediaView> getMediaView() {
        return mediaView;
    }

    public void addMediaView(MediaView mediaView) {
        this.mediaView.add(mediaView);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaUser mediaUser = (MediaUser) o;
        return id != null && Objects.equals(id, mediaUser.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
