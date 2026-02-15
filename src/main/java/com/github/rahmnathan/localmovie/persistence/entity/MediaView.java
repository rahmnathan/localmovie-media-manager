package com.github.rahmnathan.localmovie.persistence.entity;

import lombok.*;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_media_view", columnList = "media_file_id,media_user_id", unique = true))
public class MediaView implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_view_sequence_generator")
    @SequenceGenerator(name="media_view_sequence_generator", sequenceName="MEDIA_VIEW_SEQUENCE")
    private Long id;

    private Double position;
    private Double duration;

    @ManyToOne(fetch = FetchType.EAGER)
    private MediaFile mediaFile;
    @ManyToOne(fetch = FetchType.EAGER)
    private MediaUser mediaUser;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Version
    private Long version;

    public MediaView(MediaFile mediaFile, MediaUser mediaUser, Double position, Double duration) {
        this.mediaFile = mediaFile;
        this.mediaUser = mediaUser;
        this.position = position;
        this.duration = duration;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaView mediaView = (MediaView) o;
        return id != null && Objects.equals(id, mediaView.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
