package com.github.rahmnathan.localmovie.persistence.entity;

import com.github.rahmnathan.localmovie.data.MediaPath;
import lombok.*;

import jakarta.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.data.MediaPath.MEDIA_ROOT_FOLDER;

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
    private Boolean streamable;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<MediaView> mediaViews;

    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<MediaFile> children;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @ToString.Exclude
    private MediaFile parent;

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

    public static MediaFileBuilder forPath(MediaPath path){
        File file = new File(path.getRelativePath());
        return builder()
                .fileName(file.getName())
                .parentPath(file.getParent())
                .mediaFileId(UUID.randomUUID().toString())
                .absolutePath(path.getAbsolutePath())
                .streamable(path.isStreamable())
                .path(path.getRelativePath());
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
        return Objects.equals(id, mediaFile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}