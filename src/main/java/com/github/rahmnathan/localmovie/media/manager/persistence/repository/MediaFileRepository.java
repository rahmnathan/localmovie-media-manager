package com.github.rahmnathan.localmovie.media.manager.persistence.repository;

import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends CrudRepository<MediaFile, String> {
    List<MediaFile> findAllByUpdatedBeforeOrderByUpdated(LocalDateTime time);
    Optional<MediaFile> findByPath(String path);
    boolean existsByPath(String path);
}
