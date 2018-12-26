package com.github.rahmnathan.localmovie.media.manager.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.domain.MediaFileEvent;
import com.github.rahmnathan.localmovie.domain.MovieEvent;
import com.github.rahmnathan.localmovie.media.manager.repository.MediaEventRepository;
import com.github.rahmnathan.video.cast.handbrake.control.VideoController;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import io.micrometer.core.instrument.Metrics;
import net.bramp.ffmpeg.FFprobe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MediaFileEventManager implements DirectoryMonitorObserver {
    private final AtomicInteger activeConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final Logger logger = LoggerFactory.getLogger(MediaFileEventManager.class);
    private volatile Set<String> activeConversions = ConcurrentHashMap.newKeySet();
    private final PushNotificationHandler notificationHandler;
    private final MediaDataService metadataService;
    private final MediaEventRepository eventRepository;
    private final ExecutorService executorService;
    private final MediaCacheService cacheService;
    private FFprobe ffprobe;

    public MediaFileEventManager(@Value("${ffprobe.location:/usr/bin/ffprobe}") String ffprobeLocation, @Value("${concurrent.conversion.limit:1}") Integer concurrentConversions,
                                 MediaDataService mediaMetadataService, MediaEventRepository eventRepository,
                                 PushNotificationHandler notificationHandler, MediaCacheService cacheService) {
        logger.info("Number of concurrent video conversions allowed: {}", concurrentConversions);
        this.executorService = Executors.newFixedThreadPool(concurrentConversions);
        this.notificationHandler = notificationHandler;
        this.metadataService = mediaMetadataService;
        this.eventRepository = eventRepository;
        this.cacheService = cacheService;

        try {
            this.ffprobe = new FFprobe(ffprobeLocation);
        } catch (IOException e){
            logger.error("Failed to instantiate MediaFileEventManager", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCache(){
        cacheService.addEvents(eventRepository.findAll());
    }

    @Override
    public void directoryModified(WatchEvent event, Path inputPath) {
        String absolutePath = inputPath.toFile().getAbsolutePath();

        MDC.put("Path", absolutePath);
        logger.info("Detected movie event.");

        if(!activeConversions.contains(absolutePath)) {
            String relativePath = inputPath.toString().split("/LocalMedia/")[1];

            if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                waitForWriteComplete(inputPath);

                if (Files.isRegularFile(inputPath) && ffprobe != null) {
                    activeConversionGauge.getAndIncrement();
                    relativePath = launchVideoConverter(absolutePath);
                    activeConversionGauge.getAndDecrement();
                }

                cacheService.addFile(relativePath);
                getDataAndNotify(event, relativePath);
            } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE){
                cacheService.removeFile(relativePath);
                addEvent(event, relativePath);
            }
        }
    }

    @Transactional
    public void getDataAndNotify(WatchEvent event, String relativePath){
        MediaFile mediaFile = getMediaFile(event, relativePath);
        notificationHandler.sendPushNotifications(mediaFile.getMovie().getTitle(), mediaFile.getPath());
        addEvent(event, relativePath, mediaFile);
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

    private MediaFile getMediaFile(WatchEvent watchEvent, String relativePath){
        MediaFile mediaFile = metadataService.loadMediaFile(relativePath);
        if(watchEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE){
            return MediaFile.Builder.copyWithNoImage(mediaFile);
        }

        cacheService.addMedia(mediaFile);

        return mediaFile;
    }

    private void addEvent(WatchEvent watchEvent, String resultFilePath){
        logger.info("Adding event to repository.");
        MediaFileEvent event = new MediaFileEvent(MovieEvent.valueOf(watchEvent.kind().name()).getMovieEventString(), null, resultFilePath);
        cacheService.addEvent(eventRepository.saveAndFlush(event));
    }

    private void addEvent(WatchEvent watchEvent, String resultFilePath, MediaFile mediaFile){
        logger.info("Adding event to repository.");
        MediaFileEvent event = new MediaFileEvent(MovieEvent.valueOf(watchEvent.kind().name()).getMovieEventString(), mediaFile, resultFilePath);
        cacheService.addEvent(eventRepository.saveAndFlush(event));
    }

    private String launchVideoConverter(String inputFilePath){
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

        return resultFilePath.split("/LocalMedia/")[1];
    }
}
