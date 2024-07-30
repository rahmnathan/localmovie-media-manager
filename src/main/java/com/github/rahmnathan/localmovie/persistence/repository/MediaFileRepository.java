package com.github.rahmnathan.localmovie.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends CrudRepository<com.github.rahmnathan.localmovie.persistence.entity.MediaFile, String> {
    @Query("select mediaFile from MediaFile mediaFile " +
            "left join mediaFile.media media " +
            "where media.mediaType <> 'SEASON' " +
            "and mediaFile.updated < :time " +
            "order by mediaFile.updated")
    List<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> findAllByUpdatedBeforeOrderByUpdated(LocalDateTime time, Pageable pageable);

    Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> findByPath(String path);
    boolean existsByPath(String path);
    void deleteByPath(String path);
    Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> findByMediaFileId(String id);

    @Query(value = "select m1 from MediaFile m1 " +
            "left join m1.mediaViews mv " +
            "left join mv.mediaUser mu on mu.userId = :userId " +
            "where m1.mediaFileId = :mediaFileId ")
    Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> findByIdWithViews(@Param("mediaFileId") String mediaFileId, @Param("userId") String userId);
}
