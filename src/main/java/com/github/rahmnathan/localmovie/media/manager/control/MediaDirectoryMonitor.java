package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

@Service
public class MediaDirectoryMonitor {
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
                    cacheService.putFiles(path, files);
                    files.stream()
                            .map(dataService::loadMediaFile)
                            .forEach(cacheService::addMedia);
                });
    }
}
