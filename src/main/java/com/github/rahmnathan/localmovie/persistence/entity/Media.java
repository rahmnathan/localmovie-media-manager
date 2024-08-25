package com.github.rahmnathan.localmovie.persistence.entity;

import com.github.rahmnathan.omdb.data.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;

@Getter
@Setter
@Entity
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

    public static Media fromOmdbMedia(com.github.rahmnathan.omdb.data.Media inputMedia){
        Media resultMedia = new Media();
        resultMedia.actors = inputMedia.getActors();
        resultMedia.genre = inputMedia.getGenre();
        String inputImage = inputMedia.getImage();
        resultMedia.image = new MediaImage(StringUtils.isBlank(inputImage) ? new byte[0] : Base64.getDecoder().decode(inputImage), resultMedia);
        resultMedia.imdbRating = inputMedia.getImdbRating();
        resultMedia.metaRating = inputMedia.getMetaRating();
        resultMedia.mediaType = inputMedia.getMediaType();
        resultMedia.number = inputMedia.getNumber();
        resultMedia.plot = inputMedia.getPlot();
        resultMedia.releaseYear = inputMedia.getReleaseYear();
        resultMedia.title = inputMedia.getTitle();
        return resultMedia;
    }

    public com.github.rahmnathan.omdb.data.Media toOmdbMedia(){
        return com.github.rahmnathan.omdb.data.Media.builder()
                .title(this.title)
                .releaseYear(this.releaseYear)
                .plot(this.plot)
                .number(this.number)
                .mediaType(this.mediaType)
                .metaRating(this.metaRating)
                .imdbRating(this.imdbRating)
                .image(Objects.isNull(this.image) || Objects.isNull(this.image.getImage()) ? null : Base64.getEncoder().encodeToString(this.image.getImage()))
                .genre(this.genre)
                .actors(this.actors)
                .build();
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
        return 0;
    }
}
