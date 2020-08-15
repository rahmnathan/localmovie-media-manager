package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class MediaDatabaseInitializer {
    private final Logger logger = LoggerFactory.getLogger(MediaDatabaseInitializer.class);
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final MediaFileRepository mediaFileRepository;
    private final FFProbeService ffProbeService;
    private final ServiceConfig serviceConfig;
    private final MediaService dataService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        Set<MediaFile> mediaFiles = serviceConfig.getMediaPaths().stream()
                .flatMap(this::streamDirectoryTree)
                .filter(path -> path.contains(ROOT_MEDIA_FOLDER))
                .parallel()
                .flatMap(this::listFiles)
                .filter(file -> !dataService.existsInDatabase(file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1]))
                .map(this::buildMediaFile)
                .collect(Collectors.toUnmodifiableSet());

        logger.info("Saving {} new media files.", mediaFiles.size());
        mediaFileRepository.saveAll(mediaFiles);
    }

    private MediaFile buildMediaFile(File file) {
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        return MediaFile.Builder.forPath(file.getAbsolutePath())
                .setMedia(dataService.loadNewMedia(relativePath))
                .setLength(ffProbeService.loadDuration(file))
                .setMediaFileId(UUID.randomUUID().toString())
                .setAbsolutePath(file.getAbsolutePath())
                .build();
    }

    private Stream<String> streamDirectoryTree(String path) {
        Set<String> paths = new HashSet<>();
        try {
            Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    logger.info("Found media directory: {}", dir);
                    paths.add(dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Failure registering directory in directory monitor", e);
        }

        return paths.parallelStream();
    }

    private Stream<File> listFiles(String absolutePath) {
        logger.info("Listing files at - {}", absolutePath);
        File[] files = Optional.ofNullable(new File(absolutePath).listFiles()).orElse(new File[0]);
        logger.info("Found {} files.", files.length);
        return Set.of(files).parallelStream();
    }
}