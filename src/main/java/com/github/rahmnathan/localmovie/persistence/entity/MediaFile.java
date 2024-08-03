package com.github.rahmnathan.localmovie.persistence.entity;

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

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
    private Media media;

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

    public static MediaFileBuilder forPath(String path){
        String relativePath = path.split(ROOT_MEDIA_FOLDER)[1];
        File file = new File(relativePath);
        return builder()
                .fileName(file.getName())
                .parentPath(file.getParent())
                .mediaFileId(UUID.randomUUID().toString())
                .absolutePath(path)
                .path(relativePath);
    }

    @Override
    public String toString(){
        if(media != null) {
            return media.getTitle();
        }

        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaFile mediaFile = (MediaFile) o;
        return version == mediaFile.version &&
                Objects.equals(id, mediaFile.id) &&
                Objects.equals(parentPath, mediaFile.parentPath) &&
                Objects.equals(path, mediaFile.path) &&
                Objects.equals(fileName, mediaFile.fileName) &&
                Objects.equals(created, mediaFile.created) &&
                Objects.equals(updated, mediaFile.updated) &&
                Objects.equals(mediaFileId, mediaFile.mediaFileId) &&
                Objects.equals(absolutePath, mediaFile.absolutePath) &&
                Objects.equals(mediaViews, mediaFile.mediaViews) &&
                Objects.equals(media, mediaFile.media);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}