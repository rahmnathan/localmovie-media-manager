package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.rahmnathan.omdb.data.MediaType;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Data
@Entity
public class Media {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_sequence_generator")
    @SequenceGenerator(name="media_sequence_generator", sequenceName="MEDIA_SEQUENCE")
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private MediaType mediaType;
    @Lob
    private byte[] image;
    private String title;
    private String imdbRating;
    private String metaRating;
    private String releaseYear;
    private String actors;
    @Lob
    private String plot;
    private String genre;
    private Integer number;
    private LocalDateTime created;
    private LocalDateTime updated;

    @JsonIgnore
    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL)
    private MediaFile mediaFile;

    @JsonIgnore
    public MediaFile getMediaFile(){
        return mediaFile;
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
                ", mediaType=" + mediaType +
                ", image='" + (image.length != 0) + '\'' +
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

    public static Media fromOmdbMedia(com.github.rahmnathan.omdb.data.Media inputMedia, MediaFile mediaFile){
        Media resultMedia = new Media();
        resultMedia.actors = inputMedia.getActors();
        resultMedia.genre = inputMedia.getGenre();
        String inputImage = inputMedia.getImage();
        resultMedia.image = StringUtils.isEmpty(inputImage) ? new byte[0] : Base64.getDecoder().decode(inputImage);
        resultMedia.imdbRating = inputMedia.getImdbRating();
        resultMedia.metaRating = inputMedia.getMetaRating();
        resultMedia.mediaType = inputMedia.getMediaType();
        resultMedia.number = inputMedia.getNumber();
        resultMedia.plot = inputMedia.getPlot();
        resultMedia.releaseYear = inputMedia.getReleaseYear();
        resultMedia.title = inputMedia.getTitle();
        resultMedia.mediaFile = mediaFile;

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
                .image(Base64.getEncoder().encodeToString(this.image))
                .genre(this.genre)
                .actors(this.actors)
                .build();
    }
}
