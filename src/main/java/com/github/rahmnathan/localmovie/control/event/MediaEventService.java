package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.MediaFileService;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.video.cast.handbrake.control.VideoController;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import io.micrometer.core.instrument.Metrics;
import net.bramp.ffmpeg.FFprobe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.github.rahmnathan.localmovie.control.MediaDirectoryMonitor.ROOT_MEDIA_FOLDER;

@Service
public class MediaEventService {
    private final AtomicInteger queuedConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final Logger logger = LoggerFactory.getLogger(MediaEventService.class);
    private final PushNotificationService notificationHandler;
    private final MediaPersistenceService persistenceService;
    private final MediaFileService mediaFileService;
    private final ExecutorService executorService;
    private FFprobe ffprobe;

    public MediaEventService(ServiceConfig serviceConfig,
                             MediaPersistenceService persistenceService,
                             PushNotificationService notificationHandler,
                             MediaFileService mediaFileService) {
        ServiceConfig.MediaEventMonitorConfig eventMonitorConfig = serviceConfig.getDirectoryMonitor();
        logger.info("Number of concurrent video conversions allowed: {}", eventMonitorConfig.getConcurrentConversionLimit());
        this.executorService = Executors.newFixedThreadPool(eventMonitorConfig.getConcurrentConversionLimit());
        this.notificationHandler = notificationHandler;
        this.persistenceService = persistenceService;
        this.mediaFileService = mediaFileService;

        try {
            this.ffprobe = new FFprobe(eventMonitorConfig.getFfprobeLocation());
        } catch (IOException e){
            logger.error("Failed to instantiate MediaFileEventManager", e);
        }
    }

    @Transactional
    public void handleCreateEvent(File file, Set<String> activeConversions){
        logger.info("Event type: CREATE.");
        if (Files.isRegularFile(file.toPath()) && ffprobe != null) {
            queuedConversionGauge.getAndIncrement();
            launchVideoConverter(file, activeConversions);
            queuedConversionGauge.getAndDecrement();
        }

        MediaFile mediaFile = mediaFileService.loadMediaFile(file);
        addCreateEvent(file, mediaFile);
        notificationHandler.sendPushNotifications(mediaFile.getMedia().getTitle(), mediaFile.getParentPath());
    }

    @Transactional
    public void handleDeleteEvent(File file){
        logger.info("Event type: DELETE.");
        String relativePath = file.toString().split(ROOT_MEDIA_FOLDER)[1];
        if(persistenceService.existsByPath(relativePath)){
            logger.info("Removing media from database.");
            persistenceService.deleteAllByRelativePath(relativePath);
        }
    }

    private void addCreateEvent(File file, MediaFile mediaFile){
        logger.info("Adding CREATE event to repository.");
        String relativePath = file.getAbsolutePath().split(ROOT_MEDIA_FOLDER)[1];
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_CREATE.getMovieEventString(), mediaFile, relativePath);
        mediaFile.setMediaFileEvent(event);
        persistenceService.saveEvent(event);
        persistenceService.saveMediaFile(mediaFile);
    }

    private void launchVideoConverter(File file, Set<String> activeConversions){
        String inputPath = file.toString();
        String outputExtension = inputPath.endsWith(".mp4") ? ".mkv" : ".mp4";
        String resultFilePath = inputPath.substring(0, inputPath.lastIndexOf('.')) + outputExtension;

        SimpleConversionJob conversionJob = new SimpleConversionJob(ffprobe, new File(resultFilePath), file);
        logger.info("Launching video converter.");
        try {
            resultFilePath = CompletableFuture.supplyAsync(withMdc(new VideoController(conversionJob, activeConversions)), executorService).get();
            new File(resultFilePath).renameTo(file);
        } catch (InterruptedException | ExecutionException e){
            logger.error("Failure converting video.", e);
        }
    }

    private static <U> Supplier<U> withMdc(Supplier<U> supplier) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return (Supplier) () -> {
            MDC.setContextMap(mdc);
            return supplier.get();
        };
    }
}
