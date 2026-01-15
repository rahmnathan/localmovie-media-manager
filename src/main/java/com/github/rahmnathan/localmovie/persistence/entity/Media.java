package com.github.rahmnathan.localmovie.persistence.entity;

import com.github.rahmnathan.localmovie.media.omdb.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Media implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_sequence_generator")
    @SequenceGenerator(name="media_sequence_generator", sequenceName="MEDIA_SEQUENCE")
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private MediaType mediaType;
    @JoinColumn
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private MediaImage image;
    private String title;
    private String imdbRating;
    private String metaRating;
    private String releaseYear;
    private String actors;
    @JdbcTypeCode(Types.LONGNVARCHAR)
    private String plot;
    private String genre;
    private Integer number;
    private LocalDateTime created;
    private LocalDateTime updated;

    public Media(String title) {
        this.title = title;
    }

    @OneToOne(mappedBy = "media")
    private MediaFile mediaFile;

    @Version
    private Long version;

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
                ", mediaType=" + mediaType +
                ", image='" + (image != null && image.getImage() != null && image.getImage().length != 0) + '\'' +
                ", title='" + title + '\'' +
                ", imdbRating='" + imdbRating + '\'' +
                ", metaRating='" + metaRating + '\'' +
                ", releaseYear='" + releaseYear + '\'' +
                ", actors='" + actors + '\'' +
                ", plot='" + plot + '\'' +
                ", genre='" + genre + '\'' +
                ", number=" + number +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Media media = (Media) o;
        return id != null && Objects.equals(id, media.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Media cloneToPojo() {
        Media result = Media.builder()
                .actors(actors)
                .genre(genre)
                .imdbRating(imdbRating)
                .metaRating(metaRating)
                .releaseYear(releaseYear)
                .mediaType(mediaType)
                .plot(plot)
                .title(title)
                .build();

        if (image != null) {
            result.setImage(new MediaImage(image.getImage(), result));
        }

        return result;
    }
}
