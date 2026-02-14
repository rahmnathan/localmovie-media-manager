package com.github.rahmnathan.localmovie.persistence.entity;

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
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "media_subtitle",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_subtitle_media_file_lang",
           columnNames = {"media_file_id", "language_code"}))
public class MediaSubtitle implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "media_subtitle_sequence_generator")
    @SequenceGenerator(name = "media_subtitle_sequence_generator", sequenceName = "MEDIA_SUBTITLE_SEQUENCE")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    @ToString.Exclude
    private MediaFile mediaFile;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(nullable = false, length = 10)
    private String format;

    @JdbcTypeCode(Types.LONGNVARCHAR)
    @Column(name = "subtitle_content", nullable = false)
    private String subtitleContent;

    @Column(name = "opensubtitles_id", length = 50)
    private String opensubtitlesId;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Version
    private Long version;

    @PrePersist
    public void setCreated() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void setUpdated() {
        updated = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaSubtitle that = (MediaSubtitle) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MediaSubtitle{" +
                "id=" + id +
                ", languageCode='" + languageCode + '\'' +
                ", format='" + format + '\'' +
                ", opensubtitlesId='" + opensubtitlesId + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }
}
