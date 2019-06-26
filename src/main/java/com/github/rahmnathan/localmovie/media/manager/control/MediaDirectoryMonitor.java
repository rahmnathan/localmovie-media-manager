package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.media.manager.config.MediaManagerConfig;
import com.github.rahmnathan.localmovie.media.manager.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.github.rahmnathan.localmovie.media.manager.control.FileListService.ROOT_MEDIA_FOLDER;

@Service
public class MediaDirectoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaDirectoryMonitor.class);
    private final DirectoryMonitor directoryMonitor;
    private final FileListService fileListService;
    private final MediaCacheService cacheService;
    private final MediaDataService dataService;
    private final String[] mediaPaths;

    public MediaDirectoryMonitor(Collection<DirectoryMonitorObserver> observers, MediaManagerConfig mediaManagerConfig,
                                 FileListService fileListService, MediaCacheService cacheService, MediaDataService dataService) {
        this.directoryMonitor = new DirectoryMonitor(observers);
        this.mediaPaths = mediaManagerConfig.getMediaPaths();
        this.fileListService = fileListService;
        this.cacheService = cacheService;
        this.dataService = dataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        Arrays.stream(mediaPaths).forEach(directoryMonitor::registerDirectory);
        directoryMonitor.getPaths().stream()
                .parallel()
                .map(Path::toString)
                .filter(path -> path.contains(ROOT_MEDIA_FOLDER))
                .map(path -> path.split(ROOT_MEDIA_FOLDER)[1])
                .forEach(path -> {
                    Set<String> files = fileListService.listFiles(path);
                    loadMediaData(files);
                    cacheService.putFiles(path, files);
                });
    }

    @Transactional
    public void loadMediaData(Set<String> files) {
        files.parallelStream()
                .forEach(file -> {
                    try {
                        MediaFile mediaFile = dataService.loadMediaFile(file);
                        if (!dataService.existsInDatabase(mediaFile.getPath())) {
                            dataService.saveMediaFile(mediaFile);
                        }

                        cacheService.addMedia(mediaFile);
                    } catch (InvalidMediaException e) {
                        logger.error("Failure loading media data.", e);
                    }
                });
    }
}
