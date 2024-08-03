package com.github.rahmnathan.localmovie.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class MediaImage {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_image_sequence_generator")
    @SequenceGenerator(name="media_image_sequence_generator", sequenceName="MEDIA_IMAGE_SEQUENCE")
    private Long id;

    @JdbcTypeCode(Types.BINARY)
    private byte[] image;

    private LocalDateTime created;
    private LocalDateTime updated;

    @OneToOne(cascade = CascadeType.ALL)
    private Media media;

    public MediaImage(byte[] image, Media media) {
        this.image = image;
        this.media = media;
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
    public String toString() {
        return "Media{" +
                "id=" + id +
                ", image='" + (image.length != 0) + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaImage media = (MediaImage) o;
        return id != null && Objects.equals(id, media.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
