package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
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

@Service
@ConditionalOnProperty(name = "service.directoryMonitor.enabled", havingValue = "true")
public class MediaDirectoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaDirectoryMonitor.class);
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final Set<String> mediaPaths;
    private final MediaDataService dataService;

    public MediaDirectoryMonitor(ServiceConfig serviceConfig, MediaDataService dataService, Set<DirectoryMonitorObserver> observers) {
        new DirectoryMonitor(serviceConfig.getMediaPaths(), observers);
        this.mediaPaths = serviceConfig.getMediaPaths();
        this.dataService = dataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        mediaPaths.parallelStream()
                .flatMap(test -> {
                    Set<Path> paths = new HashSet<>();
                    try {
                        Files.walkFileTree(Paths.get(test), new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                logger.info("registering {} in watcher service", dir);
                                paths.add(dir);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        logger.error("Failure registering directory in directory monitor", e);
                    }

                    return paths.stream();
                })
                .map(Path::toString)
                .filter(path -> path.contains(ROOT_MEDIA_FOLDER))
                .flatMap(mediaPath -> Arrays.stream(listFiles(mediaPath)))
                .forEach(this::loadMediaData);
    }

    @Transactional
    public void loadMediaData(File file) {
        try {
            String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
            if (dataService.existsInDatabase(relativePath)) {
                logger.debug("Path already exists in database: {}", relativePath);
                return;
            }

            MediaFile mediaFile = dataService.loadMediaFile(relativePath);

            long fileSize = file.length();
            mediaFile.setLength(fileSize);

            dataService.saveMediaFile(mediaFile);
        } catch (InvalidMediaException e) {
            logger.error("Failure loading media data.", e);
        }
    }

    private File[] listFiles(String absolutePath) {
        logger.info("Listing files at - {}", absolutePath);
        File[] files = Optional.ofNullable(new File(absolutePath).listFiles()).orElse(new File[0]);
        logger.info("Found {} files.", files.length);
        return files;
    }
}