package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaImage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaImageRepository extends CrudRepository<MediaImage, Long> {

    @Query(value = "select m.image from MediaImage m where m.id = (select m2.image.id from Media m2 where m2.id = (select m3.media.id from MediaFile m3 where m3.path = :path))")
    byte[] getImageByPath(String path);

    @Query(value = "select m.image from MediaImage m where m.id = (select m2.image.id from Media m2 where m2.id = (select m3.media.id from MediaFile m3 where m3.mediaFileId = :id))")
    byte[] getImageById(String id);
}
