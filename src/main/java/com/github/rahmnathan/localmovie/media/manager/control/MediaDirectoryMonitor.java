package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

@Component
public class MediaDirectoryMonitor {
    private final DirectoryMonitor directoryMonitor;
    private final FileListProvider fileListProvider;
    private final MediaCacheService cacheService;

    public MediaDirectoryMonitor(Collection<DirectoryMonitorObserver> observers, @Value("${media.path}") String[] mediaPaths,
                                  FileListProvider fileListProvider, MediaCacheService cacheService) {
        this.fileListProvider = fileListProvider;
        this.cacheService = cacheService;
        this.directoryMonitor = new DirectoryMonitor(observers);
        Arrays.stream(mediaPaths).forEach(directoryMonitor::registerDirectory);
    }

    @PostConstruct
    public void initializeFileList(){
        directoryMonitor.getPaths().stream().map(Path::toString).forEach(path -> {
            if(path.contains(File.separator + "LocalMedia" + File.separator)){
                String relativePath = path.split(File.separator + "LocalMedia" + File.separator)[1];
                Set<String> files = fileListProvider.listFiles(relativePath);
                cacheService.putFiles(relativePath, files);
            }
        });
    }
}
