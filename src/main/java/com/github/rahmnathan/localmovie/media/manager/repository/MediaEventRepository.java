package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.localmovie.domain.MediaFileEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaEventRepository extends JpaRepository<MediaFileEvent, Long> {
}
