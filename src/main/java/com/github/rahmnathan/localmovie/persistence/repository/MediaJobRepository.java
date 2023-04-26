package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaJobRepository extends CrudRepository<MediaJob, String> {
    int countAllByStatus(String status);
    List<MediaJob> findAllByStatusOrderByCreatedAsc(String status);
    MediaJob findByInputFile(String inputFile);
}
