package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MediaUpdateService {
    private final MediaFileRepository mediaFileRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;

    public void updateMedia(String path) throws InvalidMediaException {
        MediaFile mediaFile = mediaFileRepository.findByPath(path).orElseThrow();

        Media oldMedia = mediaFile.getMedia();

        mediaFile.setMedia(mediaService.loadMedia(MediaPath.parse(path)));
        mediaFileRepository.save(mediaFile);

        mediaRepository.delete(oldMedia);
    }
}
