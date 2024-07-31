package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.Hibernate;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_media_view", columnList = "media_file_id,media_user_id", unique = true))
public class MediaView {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_view_sequence_generator")
    @SequenceGenerator(name="media_view_sequence_generator", sequenceName="MEDIA_VIEW_SEQUENCE")
    private Long id;

    private Double position;

    @ManyToOne(fetch = FetchType.EAGER)
    private MediaFile mediaFile;
    @ManyToOne(fetch = FetchType.EAGER)
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

    public MediaFile getMediaFile(){
        return mediaFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        MediaView mediaView = (MediaView) o;
        return id != null && Objects.equals(id, mediaView.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
