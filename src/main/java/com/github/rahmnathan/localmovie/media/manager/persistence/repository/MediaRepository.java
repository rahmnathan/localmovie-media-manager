package com.github.rahmnathan.localmovie.media.manager.persistence.repository;

import com.github.rahmnathan.localmovie.media.manager.persistence.entity.Media;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends CrudRepository<Media, Long> {
}
