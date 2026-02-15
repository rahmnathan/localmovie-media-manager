package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaFavoriteRepository extends JpaRepository<MediaFavorite, Long> {

    Optional<MediaFavorite> findByMediaFileMediaFileIdAndMediaUserUserId(String mediaFileId, String userId);

    boolean existsByMediaFileMediaFileIdAndMediaUserUserId(String mediaFileId, String userId);

    @Modifying
    void deleteByMediaFileMediaFileIdAndMediaUserUserId(String mediaFileId, String userId);
}
