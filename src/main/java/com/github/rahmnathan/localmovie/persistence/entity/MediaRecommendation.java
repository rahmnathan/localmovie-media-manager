package com.github.rahmnathan.localmovie.persistence.entity;

import lombok.*;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_media_recommendation_user", columnList = "media_user_id"),
        @Index(name = "idx_media_recommendation_unique", columnList = "media_file_id,media_user_id", unique = true)
})
public class MediaRecommendation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "media_recommendation_sequence_generator")
    @SequenceGenerator(name = "media_recommendation_sequence_generator", sequenceName = "MEDIA_RECOMMENDATION_SEQUENCE")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private MediaFile mediaFile;

    @ManyToOne(fetch = FetchType.EAGER)
    private MediaUser mediaUser;

    // Why this was recommended (from Ollama)
    @Column(length = 1000)
    private String reason;

    // Ranking order (1 = top recommendation)
    private Integer rank;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Version
    private Long version;

    public MediaRecommendation(MediaFile mediaFile, MediaUser mediaUser, String reason, Integer rank) {
        this.mediaFile = mediaFile;
        this.mediaUser = mediaUser;
        this.reason = reason;
        this.rank = rank;
    }

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
        MediaRecommendation that = (MediaRecommendation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
