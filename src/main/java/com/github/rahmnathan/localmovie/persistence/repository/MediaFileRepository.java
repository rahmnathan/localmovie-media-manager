package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends CrudRepository<MediaFile, String> {
    Optional<MediaFile> findByPath(String path);
    boolean existsByPath(String path);
    void deleteByPath(String path);
    Optional<MediaFile> findByMediaFileId(String id);

    @Query(value = "select m1 from MediaFile m1 " +
            "left join m1.mediaViews mv " +
            "left join mv.mediaUser mu on mu.userId = :userId " +
            "where m1.mediaFileId = :mediaFileId ")
    Optional<MediaFile> findByIdWithViews(@Param("mediaFileId") String mediaFileId, @Param("userId") String userId);

    @Query("SELECT mf FROM MediaFile mf " +
           "JOIN mf.media m " +
           "WHERE mf.streamable = true " +
           "AND m.imdbId IS NOT NULL " +
           "AND NOT EXISTS (SELECT 1 FROM MediaSubtitle ms WHERE ms.mediaFile = mf AND ms.languageCode = 'en') " +
           "AND NOT EXISTS (SELECT 1 FROM SubtitleJob sj WHERE sj.mediaFile = mf AND sj.status IN ('QUEUED', 'RUNNING'))")
    List<MediaFile> findMediaFilesNeedingSubtitles();
}
