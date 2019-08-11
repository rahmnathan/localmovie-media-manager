package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.MediaDataService;
import com.github.rahmnathan.localmovie.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileEventRepository;
import com.github.rahmnathan.video.cast.handbrake.control.VideoController;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import io.micrometer.core.instrument.Metrics;
import net.bramp.ffmpeg.FFprobe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.rahmnathan.localmovie.control.MediaDirectoryMonitor.ROOT_MEDIA_FOLDER;

@Service
public class MediaEventService {
    private final AtomicInteger queuedConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final Logger logger = LoggerFactory.getLogger(MediaEventService.class);
    private final PushNotificationService notificationHandler;
    private final MediaDataService metadataService;
    private final MediaFileEventRepository eventRepository;
    private final ExecutorService executorService;
    private FFprobe ffprobe;

    public MediaEventService(ServiceConfig serviceConfig,
                             MediaDataService mediaMetadataService, MediaFileEventRepository eventRepository,
                             PushNotificationService notificationHandler) {
        ServiceConfig.MediaEventMonitorConfig eventMonitorConfig = serviceConfig.getDirectoryMonitor();
        logger.info("Number of concurrent video conversions allowed: {}", eventMonitorConfig.getConcurrentConversionLimit());
        this.executorService = Executors.newFixedThreadPool(eventMonitorConfig.getConcurrentConversionLimit());
        this.notificationHandler = notificationHandler;
        this.metadataService = mediaMetadataService;
        this.eventRepository = eventRepository;

        try {
            this.ffprobe = new FFprobe(eventMonitorConfig.getFfprobeLocation());
        } catch (IOException e){
            logger.error("Failed to instantiate MediaFileEventManager", e);
        }
    }

    void handleCreateEvent(String relativePath, Path inputPath, Set<String> activeConversions){
        logger.info("Event type: CREATE.");

        if (Files.isRegularFile(inputPath) && ffprobe != null) {
            queuedConversionGauge.getAndIncrement();
            relativePath = launchVideoConverter(inputPath.toFile().getAbsolutePath(), activeConversions);
            queuedConversionGauge.getAndDecrement();
        }

        handleCreateEvent(relativePath);
    }

    @Transactional
    public void handleCreateEvent(String relativePath){
        try {
            MediaFile mediaFile = loadMediaFile(relativePath);
            addCreateEvent(relativePath, mediaFile);
            notificationHandler.sendPushNotifications(mediaFile.getMedia().getTitle(), mediaFile.getParentPath());
        } catch (InvalidMediaException e){
            logger.error("Failure parsing media data.", e);
        }
    }

    void handleDeleteEvent(String relativePath){
        logger.info("Event type: DELETE.");

        if(metadataService.existsInDatabase(relativePath)){
            deleteFromDatabase(relativePath);
        }

        addDeleteEvent(relativePath);
    }

    private void deleteFromDatabase(String path){
        logger.info("Removing media from database.");
        eventRepository.deleteAllByRelativePath(path);
    }

    private MediaFile loadMediaFile(String relativePath) throws InvalidMediaException {
        MediaFile mediaFile = metadataService.loadMediaFile(relativePath);
        if (!metadataService.existsInDatabase(relativePath)) {
            logger.info("Saving media to database.");
            mediaFile = metadataService.saveMediaFile(mediaFile);
        }

        return mediaFile;
    }

    private void addDeleteEvent(String resultFilePath){
        logger.info("Adding DELETE event to repository.");
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_DELETE.getMovieEventString(), null, resultFilePath);
    }

    private void addCreateEvent(String resultFilePath, MediaFile mediaFile){
        logger.info("Adding CREATE event to repository.");
        MediaFileEvent event = new MediaFileEvent(MediaEventType.ENTRY_CREATE.getMovieEventString(), mediaFile, resultFilePath);
        mediaFile.setMediaFileEvent(event);
    }

    private String launchVideoConverter(String inputFilePath, Set<String> activeConversions){
        String outputExtension = inputFilePath.endsWith(".mp4") ? ".mkv" : ".mp4";
        String resultFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.')) + outputExtension;
        SimpleConversionJob conversionJob = new SimpleConversionJob(ffprobe, new File(resultFilePath), new File(inputFilePath));

        logger.info("Launching video converter.");
        try {
            resultFilePath = CompletableFuture.supplyAsync(new VideoController(conversionJob, activeConversions), executorService).get();
        } catch (InterruptedException | ExecutionException e){
            logger.error("Failure converting video.", e);
            resultFilePath = inputFilePath;
        }

        return resultFilePath.split(ROOT_MEDIA_FOLDER)[1];
    }
}