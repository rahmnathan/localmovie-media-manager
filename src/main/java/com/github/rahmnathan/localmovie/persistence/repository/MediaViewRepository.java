package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaViewRepository extends JpaRepository<MediaView, Long> {

    @Modifying
    @Query("DELETE FROM MediaView mv WHERE mv.mediaUser.userId = :userId")
    void deleteAllByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM MediaView mv WHERE mv.mediaFile.mediaFileId = :mediaFileId AND mv.mediaUser.userId = :userId")
    void deleteByMediaFileIdAndUserId(@Param("mediaFileId") String mediaFileId, @Param("userId") String userId);
}
