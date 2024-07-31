package com.github.rahmnathan.localmovie.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.Hibernate;

import jakarta.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.control.StartupMediaInitializer.ROOT_MEDIA_FOLDER;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(
        indexes = {
                @Index(name = "idx_media_file_path", columnList = "path", unique = true),
                @Index(name = "idx_media_file_parent_path", columnList = "parentPath")},
        uniqueConstraints = {
                @UniqueConstraint(name = "media_file_id_constraint", columnNames = {"media_file_id"})
        }
)
public class MediaFile implements Serializable {

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
    private String mediaFileId;
    private String absolutePath;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<MediaView> mediaViews;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn
    private com.github.rahmnathan.localmovie.persistence.entity.Media media;

    @Version
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

    public Set<MediaView> getMediaViews() {
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

        public Builder setMedia(com.github.rahmnathan.localmovie.persistence.entity.Media media) {
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

        public Builder setMediaFileId(String mediaFileId) {
            this.mediaFile.mediaFileId = mediaFileId;
            return this;
        }

        public Builder setAbsolutePath(String path) {
            this.mediaFile.absolutePath = path;
            return this;
        }

        public MediaFile build(){
            MediaFile result = mediaFile;
            mediaFile = new MediaFile();

            return result;
        }

        public static Builder forPath(String path){
            String relativePath = path.split(ROOT_MEDIA_FOLDER)[1];
            File file = new File(relativePath);
            return Builder.newInstance()
                    .setFileName(file.getName())
                    .setParentPath(file.getParent())
                    .setMediaFileId(UUID.randomUUID().toString())
                    .setAbsolutePath(path)
                    .setPath(relativePath);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        MediaFile mediaFile = (MediaFile) o;
        return id != null && Objects.equals(id, mediaFile.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}