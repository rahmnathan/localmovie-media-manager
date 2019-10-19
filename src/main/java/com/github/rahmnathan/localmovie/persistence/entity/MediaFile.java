package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(indexes = @Index(name = "idx_media_file_path", columnList = "path", unique = true))
public class MediaFile {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_file_sequence_generator")
    @SequenceGenerator(name="media_file_sequence_generator", sequenceName="MEDIA_FILE_SEQUENCE")
    private Long id;
    private String parentPath;
    @Column(unique = true)
    private String path;
    private String fileName;
    private LocalDateTime created;
    private LocalDateTime updated;
    private int views;
    private long length;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL)
    private Set<MediaView> mediaViews;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    private Media media;

    @JsonIgnore
    @OneToOne(mappedBy = "mediaFile", cascade = CascadeType.ALL)
    private MediaFileEvent mediaFileEvent;

    @Version
    @JsonIgnore
    private long version;

    @PrePersist
    public void runPrePersist(){
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void runUpdated(){
        updated = LocalDateTime.now();
    }

    public Collection<MediaView> getMediaViews() {
        if(mediaViews == null){
            mediaViews = new HashSet<>();
        }

        return mediaViews;
    }

    public void addMediaView(MediaView mediaView) {
        this.mediaViews.add(mediaView);
    }

    @Override
    public String toString(){
        if(media != null) {
            return media.getTitle();
        }

        return fileName;
    }

    public static class Builder {
        private MediaFile mediaFile = new MediaFile();

        public static Builder newInstance(){
            return new Builder();
        }

        public Builder setFileName(String fileName) {
            this.mediaFile.fileName = fileName;
            return this;
        }

        public Builder setMedia(Media media) {
            this.mediaFile.media = media;
            return this;
        }

        public Builder setPath(String path) {
            this.mediaFile.path = path;
            return this;
        }

        public Builder setParentPath(String path) {
            this.mediaFile.parentPath = path;
            return this;
        }

        public Builder setViews(int views) {
            this.mediaFile.views = views;
            return this;
        }

        public MediaFile build(){
            MediaFile result = mediaFile;
            mediaFile = new MediaFile();

            return result;
        }

        public static Builder forPath(String path){
            File file = new File(path);
            return Builder.newInstance()
                    .setFileName(file.getName())
                    .setParentPath(file.getParent())
                    .setPath(path)
                    .setViews(0);
        }
    }
}