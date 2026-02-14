package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.data.SubtitleJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.SubtitleJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SubtitleJobRepository extends CrudRepository<SubtitleJob, Long> {
    int countAllByStatus(SubtitleJobStatus status);
    List<SubtitleJob> findAllByStatusOrderByCreatedAsc(SubtitleJobStatus status);
    List<SubtitleJob> findAllByStatus(SubtitleJobStatus status);
    boolean existsByMediaFileIdAndStatusIn(Long mediaFileId, Set<SubtitleJobStatus> statuses);
}
