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
@Table(indexes = @Index(name = "idx_media_favorite", columnList = "media_file_id,media_user_id", unique = true))
public class MediaFavorite implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_favorite_sequence_generator")
    @SequenceGenerator(name="media_favorite_sequence_generator", sequenceName="MEDIA_FAVORITE_SEQUENCE")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private MediaFile mediaFile;
    @ManyToOne(fetch = FetchType.EAGER)
    private MediaUser mediaUser;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Version
    private Long version;

    public MediaFavorite(MediaFile mediaFile, MediaUser mediaUser) {
        this.mediaFile = mediaFile;
        this.mediaUser = mediaUser;
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
        MediaFavorite mediaFavorite = (MediaFavorite) o;
        return id != null && Objects.equals(id, mediaFavorite.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
