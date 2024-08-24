package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupMediaInitializer {
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final MediaFileRepository mediaFileRepository;
    private final ServiceConfig serviceConfig;
    private final MeterRegistry meterRegistry;
    private final MediaService dataService;

    // Hold onto this for testing
    private ForkJoinTask<?> fileInitializationTask;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileListSynchronous() {
        try (ForkJoinPool customThreadPool = new ForkJoinPool(16)) {
            fileInitializationTask = customThreadPool.submit(() -> {
                long startTime = System.currentTimeMillis();

                serviceConfig.getMediaPaths().stream()
                        .parallel()
                        .flatMap(this::streamDirectoryTree)
                        .filter(path -> path.contains(ROOT_MEDIA_FOLDER))
                        .flatMap(this::listFiles)
                        .filter(file -> !dataService.existsInDatabase(file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1]))
                        .map(this::buildMediaFile)
                        .forEach(mediaFileRepository::save);

                log.info("File list initialized.");

                meterRegistry.timer("localmovies.file-list-initialization").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
            });
        }
    }

    private MediaFile buildMediaFile(File file) {
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        return MediaFile.forPath(file.getAbsolutePath())
                .media(dataService.loadNewMedia(relativePath))
                .mediaFileId(UUID.randomUUID().toString())
                .absolutePath(file.getAbsolutePath())
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

    public ForkJoinTask<?> getInitializationFuture() {
        return fileInitializationTask;
    }
}