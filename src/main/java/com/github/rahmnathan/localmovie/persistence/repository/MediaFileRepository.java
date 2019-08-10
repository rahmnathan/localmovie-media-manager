package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends CrudRepository<MediaFile, String> {
    @Query("from MediaFile mediaFile join mediaFile.media media where media.mediaType<>1 and mediaFile.updated < :time order by mediaFile.updated")
    List<MediaFile> findAllByUpdatedBeforeOrderByUpdated(LocalDateTime time, Pageable pageable);
    long countAllByParentPath(String parentPath);
    List<MediaFile> findAllByParentPath(String parentPath, Pageable pageable);
    boolean existsByPath(String path);
    Optional<MediaFile> findByPath(String path);
    @Query(value = "select lo_get(cast(image as int)) from media where id = (select media_id from media_file where path = :path)", nativeQuery = true)
    byte[] findMediaPosterByPath(String path);
    @Query(value = "select * from media_file left join media_view on media_file.id = media_view.media_file_id left join media_user on media_view.media_user_id = media_user.id and media_user.user_id = :userId where media_file.parent_path = :path", nativeQuery = true)
    List<MediaFile> findAllByParentPath(String path, String userId);
    @Query(value = "select * from media_file left join media_view on media_file.id = media_view.media_file_id left join media_user on media_view.media_user_id = media_user.id and media_user.user_id = :userId where media_file.path = :path", nativeQuery = true)
    MediaFile findByPath(String path, String userId);
}
