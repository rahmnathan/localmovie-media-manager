package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaSubtitle;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaSubtitleRepository extends CrudRepository<MediaSubtitle, Long> {

    Optional<MediaSubtitle> findByMediaFileIdAndLanguageCode(Long mediaFileId, String languageCode);

    @Query("SELECT s.subtitleContent FROM MediaSubtitle s " +
           "JOIN s.mediaFile mf " +
           "WHERE mf.mediaFileId = :mediaFileId AND s.languageCode = :languageCode")
    Optional<String> findSubtitleContentByMediaFileIdAndLanguage(
            @Param("mediaFileId") String mediaFileId,
            @Param("languageCode") String languageCode);

    boolean existsByMediaFileIdAndLanguageCode(Long mediaFileId, String languageCode);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM MediaSubtitle s JOIN s.mediaFile mf " +
           "WHERE mf.mediaFileId = :mediaFileId")
    boolean existsByMediaFileUuid(@Param("mediaFileId") String mediaFileId);
}
