package com.github.rahmnathan.localmovie.persistence.entity;

import com.github.rahmnathan.localmovie.data.SubtitleJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subtitle_job")
public class SubtitleJob {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "subtitle_job_sequence_generator")
    @SequenceGenerator(name = "subtitle_job_sequence_generator", sequenceName = "SUBTITLE_JOB_SEQUENCE")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    @ToString.Exclude
    private MediaFile mediaFile;

    @Column(name = "imdb_id", length = 20)
    private String imdbId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubtitleJobStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Version
    private Long version;

    @PrePersist
    public void runPrePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
        if (status == null) {
            status = SubtitleJobStatus.QUEUED;
        }
    }

    @PreUpdate
    public void runUpdated() {
        updated = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtitleJob that = (SubtitleJob) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
