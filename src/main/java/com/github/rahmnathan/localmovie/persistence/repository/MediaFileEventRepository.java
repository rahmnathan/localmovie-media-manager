package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaFileEventRepository extends JpaRepository<MediaFileEvent, Long> {
    void deleteAllByRelativePath(String path);
    List<MediaFileEvent> findAllByTimestampAfterOrderByTimestamp(LocalDateTime localDateTime, Pageable pageable);
}
