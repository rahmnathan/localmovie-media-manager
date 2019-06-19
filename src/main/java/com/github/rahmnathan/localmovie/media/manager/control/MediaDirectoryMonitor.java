package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.exception.InvalidMediaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Service
public class MediaDirectoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MediaDirectoryMonitor.class);
    private final DirectoryMonitor directoryMonitor;
    private final FileListProvider fileListProvider;
    private final MediaCacheService cacheService;
    private final MediaDataService dataService;
    private final String[] mediaPaths;

    public MediaDirectoryMonitor(Collection<DirectoryMonitorObserver> observers, @Value("${media.path}") String[] mediaPaths,
                                 FileListProvider fileListProvider, MediaCacheService cacheService, MediaDataService dataService) {
        this.directoryMonitor = new DirectoryMonitor(observers);
        this.fileListProvider = fileListProvider;
        this.cacheService = cacheService;
        this.dataService = dataService;
        this.mediaPaths = mediaPaths;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileList() {
        Arrays.stream(mediaPaths).forEach(directoryMonitor::registerDirectory);
        directoryMonitor.getPaths().stream()
                .parallel()
                .map(Path::toString)
                .filter(path -> path.contains(File.separator + "LocalMedia" + File.separator))
                .map(path -> path.split(File.separator + "LocalMedia" + File.separator)[1])
                .forEach(path -> {
                    Set<String> files = fileListProvider.listFiles(path);
                    loadMediaData(files);
                    cacheService.putFiles(path, files);
                });
    }

    private void loadMediaData(Set<String> files) {
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
