package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "service.directoryMonitor.enabled", havingValue = "true")
public class MediaDirectoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaDirectoryMonitor.class);
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final MediaFileRepository mediaFileRepository;
    private final Set<DirectoryMonitorObserver> observers;
    private final MediaDataService dataService;
    private final ServiceConfig serviceConfig;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        new DirectoryMonitor(serviceConfig.getMediaPaths(), observers);
        Set<MediaFile> newMediaFiles = serviceConfig.getMediaPaths().parallelStream()
                .map(Paths::get)
                .flatMap(this::streamDirectoryTree)
                .map(Path::toString)
                .filter(path -> path.contains(ROOT_MEDIA_FOLDER))
                .flatMap(this::listFiles)
                .filter(file -> !dataService.existsInDatabase(file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1]))
                .map(this::buildMediaFile)
                .collect(Collectors.toUnmodifiableSet());

        mediaFileRepository.saveAll(newMediaFiles);
    }

    private MediaFile buildMediaFile(File file) {
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        return MediaFile.Builder.forPath(relativePath)
                .setMedia(dataService.loadMedia(relativePath))
                .setLength(file.length())
                .build();
    }

    private Stream<Path> streamDirectoryTree(Path path) {
        Set<Path> paths = new HashSet<>();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    logger.info("Found media directory: {}", dir);
                    paths.add(dir);
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