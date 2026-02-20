package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface MediaFavoriteRepository extends JpaRepository<MediaFavorite, Long> {

    Optional<MediaFavorite> findByMediaFileMediaFileIdAndMediaUserUserId(String mediaFileId, String userId);

    boolean existsByMediaFileMediaFileIdAndMediaUserUserId(String mediaFileId, String userId);

    @Modifying
    void deleteByMediaFileMediaFileIdAndMediaUserUserId(String mediaFileId, String userId);

    @Query("SELECT mf.mediaFile.mediaFileId FROM MediaFavorite mf " +
            "WHERE mf.mediaUser.userId = :userId AND mf.mediaFile.mediaFileId IN :mediaFileIds")
    Set<String> findMediaFileIdsByUserIdAndMediaFileIds(@Param("userId") String userId, @Param("mediaFileIds") Set<String> mediaFileIds);
}
