package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MediaRecommendationRepository extends JpaRepository<MediaRecommendation, Long> {

    List<MediaRecommendation> findByMediaUserUserIdOrderByRankAsc(String userId);

    @Modifying
    void deleteByMediaUserUserId(String userId);

    @Query("SELECT mr.mediaFile.mediaFileId FROM MediaRecommendation mr WHERE mr.mediaUser.userId = :userId")
    Set<String> findAllMediaFileIdsByUserId(@Param("userId") String userId);

    boolean existsByMediaUserUserId(String userId);
}
