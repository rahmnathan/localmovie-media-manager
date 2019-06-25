package com.github.rahmnathan.localmovie.media.manager.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Calendar;

@Entity
public class MediaFile {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_file_sequence_generator")
    @SequenceGenerator(name="media_file_sequence_generator", sequenceName="MEDIA_FILE_SEQUENCE")
    private Long id;
    private String path;
    private String fileName;
    private long created;
    private LocalDateTime updated;
    private int views;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    private Media media;

    @JsonIgnore
    @OneToOne(mappedBy = "mediaFile")
    private MediaFileEvent mediaFileEvent;

    @Version
    @JsonIgnore
    private long version;

    private MediaFile(String path, Media media, int views, String fileName) {
        this.path = path;
        this.media = media;
        this.fileName = fileName;
        this.views = views;
    }

    public MediaFile(){
        // Default constructor
    }

    @PrePersist
    public void runPrePersist(){
        created = Calendar.getInstance().getTimeInMillis();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void runUpdated(){
        updated = LocalDateTime.now();
    }

    public Long getId(){
        return id;
    }

    public LocalDateTime getUpdated(){
        return updated;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    public void addView(){
        views++;
    }

    public int getViews() {
        return views;
    }

    public long getCreated() {
        return created;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media){
        this.media = media;
    }

    public void deleteMediaFileEvent(){
        this.mediaFileEvent = null;
    }

    public MediaFileEvent getMediaFileEvent() {
        return mediaFileEvent;
    }

    public void setMediaFileEvent(MediaFileEvent mediaFileEvent) {
        this.mediaFileEvent = mediaFileEvent;
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
            String fileName = new File(path).getName();
            return Builder.newInstance()
                    .setFileName(fileName)
                    .setPath(path)
                    .setViews(0);
        }
    }
}