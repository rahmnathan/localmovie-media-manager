package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
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
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "service.directoryMonitor.enabled", havingValue = "true")
public class MediaDirectoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaDirectoryMonitor.class);
    public static final String ROOT_MEDIA_FOLDER = File.separator + "LocalMedia" + File.separator;
    private final DirectoryMonitor directoryMonitor;
    private final MediaDataService dataService;
    private final String[] mediaPaths;

    public MediaDirectoryMonitor(ServiceConfig serviceConfig, MediaDataService dataService) {
        this.mediaPaths = serviceConfig.getMediaPaths();
        this.directoryMonitor = new DirectoryMonitor();
        this.dataService = dataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        Arrays.stream(mediaPaths).forEach(directoryMonitor::registerDirectory);
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
            MediaFile mediaFile = dataService.loadMediaFile(relativePath);

            long fileSize = file.length();
            mediaFile.setLength(fileSize);

            if (!dataService.existsInDatabase(mediaFile.getPath())) {
                dataService.saveMediaFile(mediaFile);
            }
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