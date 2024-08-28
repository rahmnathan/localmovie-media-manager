package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.media.MediaInitializer.ROOT_MEDIA_FOLDER;

@Slf4j
@Service
@AllArgsConstructor
public class MediaFileService {
    private final MediaFileRepository mediaFileRepository;
    private final MediaService mediaService;

    @Transactional
    public MediaFile loadMediaFile(File file){
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        Optional<MediaFile> mediaFile = mediaFileRepository.findByPath(relativePath);
        if (mediaFile.isPresent()) {
            log.info("Getting from database - {}", relativePath);
            return mediaFile.get();
        }

        return loadNewMediaFile(file, relativePath);
    }

    private MediaFile loadNewMediaFile(File file, String relativePath){
        MediaFile mediaFile = MediaFile.forPath(file.getAbsolutePath())
                .media(mediaService.loadMedia(relativePath))
                .mediaFileId(UUID.randomUUID().toString())
                .absolutePath(file.getAbsolutePath())
                .build();

        return mediaFileRepository.save(mediaFile);
    }
}
