package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@AllArgsConstructor
public class StartupMediaInitializer {
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final MediaFileRepository mediaFileRepository;
    private final FFProbeService ffProbeService;
    private final ServiceConfig serviceConfig;
    private final MediaService dataService;

    @Timed
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

        log.info("Saving {} new media files.", mediaFiles.size());
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
                    log.info("Found media directory: {}", dir);
                    paths.add(dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failure registering directory in directory monitor", e);
        }

        return paths.parallelStream();
    }

    private Stream<File> listFiles(String absolutePath) {
        log.info("Listing files at - {}", absolutePath);
        File[] files = Optional.ofNullable(new File(absolutePath).listFiles()).orElse(new File[0]);
        log.info("Found {} files.", files.length);
        return Set.of(files).parallelStream();
    }
}