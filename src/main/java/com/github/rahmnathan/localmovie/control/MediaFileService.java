package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.control.MediaDatabaseInitializer.ROOT_MEDIA_FOLDER;

@Service
@AllArgsConstructor
public class MediaFileService {
    private final Logger logger = LoggerFactory.getLogger(MediaFileService.class);
    private final MediaFileRepository mediaFileRepository;
    private final MediaService mediaService;

    @Transactional
    public MediaFile loadMediaFile(File file){
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        Optional<MediaFile> mediaFile = mediaFileRepository.findByPath(relativePath);
        if (mediaFile.isPresent()) {
            logger.info("Getting from database - {}", relativePath);
            return mediaFile.get();
        }

        return loadNewMediaFile(file, relativePath);
    }

    private MediaFile loadNewMediaFile(File file, String relativePath){
        MediaFile mediaFile = MediaFile.Builder.forPath(file.getAbsolutePath())
                .setMedia(mediaService.loadNewMedia(relativePath))
                .setLength(file.length())
                .setMediaFileId(UUID.randomUUID().toString())
                .setAbsolutePath(file.getAbsolutePath())
                .build();

        return mediaFileRepository.save(mediaFile);
    }
}
