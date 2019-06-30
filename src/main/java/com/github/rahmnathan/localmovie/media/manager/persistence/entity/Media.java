package com.github.rahmnathan.localmovie.media.manager.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.rahmnathan.omdb.data.MediaType;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Media {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_sequence_generator")
    @SequenceGenerator(name="media_sequence_generator", sequenceName="MEDIA_SEQUENCE")
    private Long id;

    private MediaType mediaType;
    @Lob
    private String image;
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

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public String getMetaRating() {
        return metaRating;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public String getActors() {
        return actors;
    }

    public String getPlot() {
        return plot;
    }

    public String getGenre() {
        return genre;
    }

    public Integer getNumber() {
        return number;
    }

    public Long getId(){
        return id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    @JsonIgnore
    public MediaFile getMediaFile(){
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile){
        this.mediaFile = mediaFile;
    }

    public void removeMediaFile(){
        this.mediaFile = null;
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
                ", image='" + !StringUtils.isEmpty(image) + '\'' +
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
        resultMedia.image = inputMedia.getImage();
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
        return com.github.rahmnathan.omdb.data.Media.Builder.newInstance()
                .setTitle(this.title)
                .setReleaseYear(this.releaseYear)
                .setPlot(this.plot)
                .setNumber(this.number)
                .setMediaType(this.mediaType)
                .setMetaRating(this.metaRating)
                .setIMDBRating(this.imdbRating)
                .setImage(this.image)
                .setGenre(this.genre)
                .setActors(this.actors)
                .build();
    }
}
