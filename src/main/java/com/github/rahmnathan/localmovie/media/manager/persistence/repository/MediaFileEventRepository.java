package com.github.rahmnathan.localmovie.media.manager.persistence.repository;

import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFileEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileEventRepository extends JpaRepository<MediaFileEvent, Long> {
    void deleteAllByRelativePath(String path);
}
