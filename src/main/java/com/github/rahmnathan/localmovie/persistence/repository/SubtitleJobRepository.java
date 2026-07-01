package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.data.SubtitleJobStatus;
import com.github.rahmnathan.localmovie.data.SubtitleSyncStatus;
import com.github.rahmnathan.localmovie.persistence.entity.SubtitleJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface SubtitleJobRepository extends CrudRepository<SubtitleJob, Long> {
    int countAllByStatus(SubtitleJobStatus status);
    @EntityGraph(attributePaths = "mediaFile")
    List<SubtitleJob> findAllByStatusOrderByCreatedAsc(SubtitleJobStatus status);
    @EntityGraph(attributePaths = "mediaFile")
    List<SubtitleJob> findTop5ByStatusOrderByCreatedAsc(SubtitleJobStatus status);
    List<SubtitleJob> findAllByStatusAndUpdatedBefore(SubtitleJobStatus status, LocalDateTime updatedBefore);
    List<SubtitleJob> findAllByStatus(SubtitleJobStatus status);
    boolean existsByMediaFileIdAndStatusIn(Long mediaFileId, Set<SubtitleJobStatus> statuses);
    @EntityGraph(attributePaths = "mediaFile")
    List<SubtitleJob> findAllBySyncStatus(SubtitleSyncStatus syncStatus);
}
