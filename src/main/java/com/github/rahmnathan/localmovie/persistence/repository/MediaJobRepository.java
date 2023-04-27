package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MediaJobRepository extends CrudRepository<MediaJob, String> {
    int countAllByStatus(String status);
    List<MediaJob> findAllByStatusOrderByCreatedAsc(String status);
    boolean existsByOutputFileAndStatusIn(String outputFile, Set<String> statuses);
    boolean existsByInputFileAndStatusIn(String inputFile, Set<String> statuses);
    MediaJob findByJobId(String jobId);
}
