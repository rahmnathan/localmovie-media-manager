package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.RedactedMediaFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends CrudRepository<MediaFile, String> {
    @Query("select mediaFile from MediaFile mediaFile " +
            "left join mediaFile.media media " +
            "where media.mediaType<>1 " +
            "and mediaFile.updated < :time " +
            "order by mediaFile.updated")
    List<MediaFile> findAllByUpdatedBeforeOrderByUpdated(LocalDateTime time, Pageable pageable);

    @Query(value = "select m1 from MediaFile m1 " +
            "left join m1.mediaViews mv " +
            "left join mv.mediaUser mu on mu.userId = :userId " +
            "where m1.parentPath = :path ")
    List<MediaFile> findAllByParentPath(String path, String userId, Pageable pageable);

    @Query(value = "select m1 from MediaFile m1 " +
            "left join m1.mediaViews mv " +
            "left join mv.mediaUser mu on mu.userId = :userId " +
            "where m1.parentPath = :path ")
    List<RedactedMediaFile> findAllByParentPathNoPoster(@Param("path") String path, @Param("userId") String userId, Pageable pageable);

    @Query(value = "select m1 from MediaFile m1 " +
            "left join m1.mediaViews mv " +
            "left join mv.mediaUser mu on mu.userId = :userId " +
            "where m1.path = :path ")
    MediaFile findByPath(@Param("path") String path, @Param("userId") String userId);

    long countAllByParentPath(String parentPath);
    Optional<MediaFile> findByPath(String path);
    boolean existsByPath(String path);
    void deleteByPath(String path);
}
