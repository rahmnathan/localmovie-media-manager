package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.event.files.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static com.github.rahmnathan.localmovie.web.filter.LoggingFilter.X_CORRELATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaEventMonitor implements DirectoryMonitorObserver {

    public static final Set<String> ACTIVE_STATUSES = Set.of(MediaJobStatus.QUEUED.name(), MediaJobStatus.RUNNING.name());
    private static final String HANDBRAKE_PRESET = "Chromecast 1080p60 Surround";

    private final MediaJobRepository mediaJobRepository;
    private final MediaEventService eventService;
    private final ServiceConfig serviceConfig;
    private final LockRegistry lockRegistry;

    @Override
    public void directoryModified(WatchEvent.Kind event, File file) throws InvalidMediaException, IOException {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        String absolutePath = file.getAbsolutePath();

        // Only process events on one instance
        Lock directoryModificationLock = lockRegistry.obtain(event.name() + ":" + absolutePath);
        boolean lockAcquired = directoryModificationLock.tryLock();
        if (!lockAcquired) {
            log.info("Lock ({}) is currently held by another instance.", directoryModificationLock);
            MDC.clear();
            return;
        }

        try {
            if (absolutePath.endsWith("partial~") || isActiveConversion(file)) {
                log.info("File {} is currently being converted. Skipping event.", absolutePath);
                return;
            }

            MediaPath path = MediaPath.parse(file);

            if (event == StandardWatchEventKinds.ENTRY_CREATE) {
                waitForWriteComplete(file);
                if (Files.isRegularFile(file.toPath()) && serviceConfig.getConversionService().isEnabled()) {
                    Path outputPath = Paths.get(path.getDestinationPath());
                    if (Files.exists(outputPath)) {
                        log.info("Deleting existing output file: {}", path.getDestinationPath());
                        Files.delete(outputPath);
                    }

                    queueConversionJob(path);
                } else {
                    eventService.handleCreateEvent(path);
                }
            } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
                eventService.handleDeleteEvent(path);
            }
        } finally {
            directoryModificationLock.unlock();
        }

        MDC.clear();
    }

    private void queueConversionJob(MediaPath path) {
        String jobId = path.asJobId();
        log.info("Queuing video conversion jobId: {}", jobId);

        MediaJob mediaJob = MediaJob.builder()
                .inputFile(path.getAbsolutePath())
                .outputFile(path.getDestinationPath())
                .jobId(jobId)
                .handbrakePreset(HANDBRAKE_PRESET)
                .status(MediaJobStatus.QUEUED.name())
                .build();

        mediaJobRepository.save(mediaJob);
    }

    private boolean isActiveConversion(File file) {
        return mediaJobRepository.existsByOutputFileAndStatusIn(file.toString(), ACTIVE_STATUSES) ||
                mediaJobRepository.existsByInputFileAndStatusIn(file.toString(), ACTIVE_STATUSES);
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
