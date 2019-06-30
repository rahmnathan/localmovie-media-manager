package com.github.rahmnathan.localmovie.media.manager.persistence.repository;

import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends CrudRepository<MediaFile, String> {
    @Query("from MediaFile mediaFile join mediaFile.media media where media.mediaType<>1 and mediaFile.updated < :time order by mediaFile.updated")
    List<MediaFile> findAllByUpdatedBeforeOrderByUpdated(@Param("time") LocalDateTime time, Pageable pageable);
    Optional<MediaFile> findByPath(String path);
    boolean existsByPath(String path);
}
