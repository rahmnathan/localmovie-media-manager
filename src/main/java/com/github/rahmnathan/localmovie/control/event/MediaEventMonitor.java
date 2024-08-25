package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Set;
import java.util.UUID;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Slf4j
@Service
@AllArgsConstructor
public class MediaEventMonitor implements DirectoryMonitorObserver {

    private static final Set<String> ACTIVE_STATUSES = Set.of(MediaJobStatus.QUEUED.name(), MediaJobStatus.RUNNING.name());
    private static final String HANDBRAKE_PRESET = "Chromecast 1080p60 Surround";

    private final MediaJobRepository mediaJobRepository;
    private final MediaEventService eventService;

    @Override
    public void directoryModified(WatchEvent.Kind event, File file) {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        String absolutePath = file.getAbsolutePath();
        log.info("Detected media event {} at path: {}", event.name(), absolutePath);

        if(absolutePath.endsWith("partial~") || isActiveConversion(file)) {
            log.info("File {} is currently being converted. Skipping event.", absolutePath);
            return;
        }

        if (event == StandardWatchEventKinds.ENTRY_CREATE) {
            waitForWriteComplete(file);
            if (Files.isRegularFile(file.toPath())) {
                queueConversionJob(file);
            } else {
                eventService.handleCreateEvent(file);
            }
        } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
            eventService.handleDeleteEvent(file);
        }

        MDC.clear();
    }

    private void queueConversionJob(File file) {
        String inputPath = file.toString();
        String jobId = formatPath(inputPath);
        String outputPath = inputPath.substring(0, inputPath.lastIndexOf('.')) + (inputPath.endsWith(".mp4") ? ".mkv" : ".mp4");

        log.info("Queuing video conversion jobId: {}", jobId);

        MediaJob mediaJob = MediaJob.builder()
                .inputFile(inputPath)
                .outputFile(outputPath)
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

    private String formatPath(String path) {
        return path.split(File.separator + "LocalMedia" + File.separator)[1].replaceAll("[^A-Za-z0-9]", "-");
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
