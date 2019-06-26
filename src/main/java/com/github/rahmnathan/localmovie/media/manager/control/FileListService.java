package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.localmovie.media.manager.config.MediaManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileListService {
    private final Logger logger = LoggerFactory.getLogger(FileListService.class.getName());
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final String[] mediaPaths;

    public FileListService(MediaManagerConfig mediaManagerConfig) {
        this.mediaPaths = mediaManagerConfig.getMediaPaths();
    }

    Set<String> listFiles(String path) {
        logger.info("Listing files at - {}", path);

        return Arrays.stream(mediaPaths)
                .map(mediaPath -> Optional.ofNullable(new File(mediaPath + path).listFiles()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(files -> logger.info("Found {} files.", files.length))
                .flatMap(Arrays::stream)
                .map(file -> file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1])
                .collect(Collectors.toSet());
    }
}
