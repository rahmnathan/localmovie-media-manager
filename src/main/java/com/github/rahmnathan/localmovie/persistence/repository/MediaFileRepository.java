package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
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
    @Query("from MediaFile mediaFile join mediaFile.media media where media.mediaType<>1 and mediaFile.updated < :time order by mediaFile.updated")
    List<MediaFile> findAllByUpdatedBeforeOrderByUpdated(LocalDateTime time, Pageable pageable);

    @Query(value = "select lo_get(cast(image as int)) from media where id = (select media_id from media_file where path = :path)", nativeQuery = true)
    byte[] findMediaPosterByPath(String path);

    @Query(value = "select * from media_file " +
            "left join media_view on media_file.id = media_view.media_file_id " +
            "left join media_user on media_view.media_user_id = media_user.id and media_user.user_id = :userId " +
            "where media_file.parent_path = :path order by :order offset :first limit :last",
            countQuery = "select count(*) from media_file " +
                    "where media_file.parent_path = :path",
            nativeQuery = true)
    List<MediaFile> findAllByParentPath(
            @Param("path") String path,
            @Param("userId") String userId,
            String order,
            Integer first,
            Integer last);

    @Query(value = "select * from media_file " +
            "left join media_view on media_file.id = media_view.media_file_id " +
            "left join media_user on media_view.media_user_id = media_user.id and media_user.user_id = :userId " +
            "where media_file.path = :path",
            nativeQuery = true)
    MediaFile findByPath(@Param("path") String path, @Param("userId") String userId);

    long countAllByParentPath(String parentPath);
    Optional<MediaFile> findByPath(String path);
    boolean existsByPath(String path);
}
