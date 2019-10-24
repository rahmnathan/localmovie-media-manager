package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.Media;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends CrudRepository<Media, Long> {

    @Query(value = "select m.image from Media m where m.id = (select m2.media from MediaFile m2 where m2.path = :path)")
    byte[] getImageByPath(String path);
}
