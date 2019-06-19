package com.github.rahmnathan.localmovie.media.manager.repository;

import com.github.rahmnathan.localmovie.domain.MediaFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaRepository extends CrudRepository<MediaFile, String> {
    List<MediaFile> findAllByUpdatedBefore(LocalDateTime time);
}
