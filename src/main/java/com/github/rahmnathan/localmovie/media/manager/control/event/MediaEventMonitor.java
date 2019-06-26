package com.github.rahmnathan.localmovie.media.manager.control.event;

import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rahmnathan.localmovie.media.manager.control.FileListService.ROOT_MEDIA_FOLDER;

@Service
public class MediaEventMonitor implements DirectoryMonitorObserver {
    private final Logger logger = LoggerFactory.getLogger(MediaEventMonitor.class);
    private volatile Set<String> activeConversions = ConcurrentHashMap.newKeySet();
    private final MediaEventService eventService;

    public MediaEventMonitor(MediaEventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void directoryModified(WatchEvent event, Path inputPath) {
        String absolutePath = inputPath.toFile().getAbsolutePath();

        MDC.put("Path", absolutePath);
        logger.info("Detected media event.");

        if(!activeConversions.contains(absolutePath)) {
            String relativePath = inputPath.toString().split(ROOT_MEDIA_FOLDER)[1];

            if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                waitForWriteComplete(inputPath);
                eventService.handleCreateEvent(relativePath, inputPath, activeConversions);
            } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE){
                eventService.handleDeleteEvent(relativePath);
            }
        } else {
            logger.info("Media is being converted. Ignoring.");
        }
    }

    private void waitForWriteComplete(Path filePath){
        while(true) {
            File file = filePath.toFile();
            long beforeLastModified = file.lastModified();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e){
                logger.error("Failure waiting for file to finish writing", e);
            }

            long afterLastModified = file.lastModified();

            if(beforeLastModified == afterLastModified){
                return;
            }
        }
    }
}
