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
import java.nio.file.Path;
import java.util.*;

@Service
@ConditionalOnProperty(name = "service.directoryMonitor.enabled", havingValue = "true")
public class MediaDirectoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaDirectoryMonitor.class);
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final DirectoryMonitor directoryMonitor;
    private final MediaDataService dataService;

    public MediaDirectoryMonitor(ServiceConfig serviceConfig, MediaDataService dataService, Set<DirectoryMonitorObserver> observers) {
        this.directoryMonitor = new DirectoryMonitor(serviceConfig.getMediaPaths(), observers);
        this.dataService = dataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        directoryMonitor.getPaths().parallelStream()
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