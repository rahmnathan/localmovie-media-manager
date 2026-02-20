package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Query("SELECT COUNT(mf) FROM MediaFile mf " +
           "JOIN mf.media m " +
           "WHERE mf.mediaFileType IN (com.github.rahmnathan.localmovie.data.MediaFileType.MOVIE, " +
           "                           com.github.rahmnathan.localmovie.data.MediaFileType.SERIES) " +
           "AND mf.mediaFileId NOT IN :excludeIds " +
           "AND m.title IS NOT NULL " +
           "AND m.genre IS NOT NULL")
    long countCandidatesForRecommendation(@Param("excludeIds") Set<String> excludeIds);

    @Query("SELECT mf FROM MediaFile mf " +
           "JOIN FETCH mf.media m " +
           "WHERE mf.mediaFileType IN (com.github.rahmnathan.localmovie.data.MediaFileType.MOVIE, " +
           "                           com.github.rahmnathan.localmovie.data.MediaFileType.SERIES) " +
           "AND mf.mediaFileId NOT IN :excludeIds " +
           "AND m.title IS NOT NULL " +
           "AND m.genre IS NOT NULL " +
           "ORDER BY mf.id ASC")
    List<MediaFile> findCandidatesForRecommendationPage(@Param("excludeIds") Set<String> excludeIds, Pageable pageable);

    @Query("SELECT mf FROM MediaFile mf " +
           "LEFT JOIN FETCH mf.media " +
           "LEFT JOIN FETCH mf.parent " +
           "WHERE mf.mediaFileId IN :mediaFileIds")
    List<MediaFile> findByMediaFileIdInWithMediaAndParent(@Param("mediaFileIds") List<String> mediaFileIds);
}
