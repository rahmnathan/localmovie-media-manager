package com.github.rahmnathan.localmovie.control.event;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rahmnathan.localmovie.control.MediaDirectoryMonitor.ROOT_MEDIA_FOLDER;

@Service
public class MediaEventMonitor implements DirectoryMonitorObserver {
    private final Logger logger = LoggerFactory.getLogger(MediaEventMonitor.class);
    private volatile Set<String> activeConversions = ConcurrentHashMap.newKeySet();
    private final MediaEventService eventService;

    public MediaEventMonitor(MediaEventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void directoryModified(WatchEvent.Kind event, File file) {
        String absolutePath = file.getAbsolutePath();
        MDC.put("correlation-id", UUID.randomUUID().toString());
        logger.info("Detected media event at path: {}", absolutePath);

        if (!activeConversions.contains(absolutePath)) {
            if (event == StandardWatchEventKinds.ENTRY_CREATE) {
                waitForWriteComplete(file);
                eventService.handleCreateEvent(file, activeConversions);
            } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
                eventService.handleDeleteEvent(file);
            }
        } else {
            logger.info("Media is being converted. Ignoring.");
        }
    }

    private void waitForWriteComplete(File file) {
        while (true) {
            long beforeLastModified = file.lastModified();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.error("Failure waiting for file to finish writing", e);
            }

            long afterLastModified = file.lastModified();
            if (beforeLastModified == afterLastModified) {
                return;
            }
        }
    }
}
