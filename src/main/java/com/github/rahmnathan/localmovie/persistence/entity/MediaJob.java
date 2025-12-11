package com.github.rahmnathan.localmovie.persistence.entity;

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
public class MediaJob {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="media_file_sequence_generator")
    @SequenceGenerator(name="media_file_sequence_generator", sequenceName="MEDIA_FILE_SEQUENCE")
    private Long id;

    private String handbrakePreset;
    private String outputFile;
    private String inputFile;
    private String status;
    private String jobId;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Version
    private Long version;

    @PrePersist
    public void runPrePersist(){
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void runUpdated(){
        updated = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
