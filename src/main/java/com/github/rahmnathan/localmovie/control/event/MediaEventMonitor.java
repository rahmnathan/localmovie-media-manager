package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.job.MediaJobService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Slf4j
@Service
public class MediaEventMonitor implements DirectoryMonitorObserver {
    private final MediaEventService eventService;
    private final MediaJobService mediaJobService;

    public MediaEventMonitor(MediaEventService eventService, ServiceConfig serviceConfig, MediaJobService mediaJobService) throws IOException {
        ServiceConfig.MediaEventMonitorConfig eventMonitorConfig = serviceConfig.getDirectoryMonitor();
        log.info("Number of concurrent video conversions allowed: {}", eventMonitorConfig.getConcurrentConversionLimit());
        this.mediaJobService = mediaJobService;
        this.eventService = eventService;
    }

    @Override
    public void directoryModified(WatchEvent.Kind event, File file) {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        String absolutePath = file.getAbsolutePath();
        log.info("Detected media event {} at path: {}", event.name(), absolutePath);

        if(absolutePath.endsWith("partial~") || mediaJobService.isActiveConversion(file)) {
            log.info("File {} is currently being converted. Skipping event.", absolutePath);
            return;
        }

        if (event == StandardWatchEventKinds.ENTRY_CREATE) {
            waitForWriteComplete(file);
            if (Files.isRegularFile(file.toPath())) {
                mediaJobService.createConversionJob(file);
            } else {
                eventService.handleCreateEvent(file);
            }
        } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
            eventService.handleDeleteEvent(file);
        }

        MDC.clear();
    }

    private void waitForWriteComplete(File file) {
        while (true) {
            long beforeLastModified = file.lastModified();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("Thread interrupted waiting for file write to complete.", e);
                Thread.currentThread().interrupt();
            }

            long afterLastModified = file.lastModified();
            if (beforeLastModified == afterLastModified) {
                return;
            }
        }
    }
}
