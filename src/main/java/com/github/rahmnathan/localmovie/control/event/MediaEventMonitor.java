package com.github.rahmnathan.localmovie.control.event;

import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.video.cast.handbrake.control.VideoController;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Service
public class MediaEventMonitor implements DirectoryMonitorObserver {
    private final AtomicInteger queuedConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final Logger logger = LoggerFactory.getLogger(MediaEventMonitor.class);
    private volatile Set<String> activeConversions = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;

    private final MediaEventService eventService;

    public MediaEventMonitor(MediaEventService eventService, ServiceConfig serviceConfig) {
        ServiceConfig.MediaEventMonitorConfig eventMonitorConfig = serviceConfig.getDirectoryMonitor();
        logger.info("Number of concurrent video conversions allowed: {}", eventMonitorConfig.getConcurrentConversionLimit());
        this.executorService = Executors.newFixedThreadPool(eventMonitorConfig.getConcurrentConversionLimit());
        this.eventService = eventService;
    }

    @Override
    public void directoryModified(WatchEvent.Kind event, File file) {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        String absolutePath = file.getAbsolutePath();
        logger.info("Detected media event {} at path: {}", event.name(), absolutePath);
        if (activeConversions.contains(absolutePath)) {
            logger.info("Media is being converted. Ignoring.");
            return;
        }

        if (event == StandardWatchEventKinds.ENTRY_CREATE) {
            waitForWriteComplete(file);
            if (Files.isRegularFile(file.toPath())) {
                launchVideoConverter(file, activeConversions);
            }

            eventService.handleCreateEvent(file);
        } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
            eventService.handleDeleteEvent(file);
        }

        MDC.clear();
    }

    private void launchVideoConverter(File file, Set<String> activeConversions){
        String inputPath = file.toString();
        String resultFilePath = inputPath.substring(0, inputPath.lastIndexOf('.')) + (inputPath.endsWith(".mp4") ? ".mkv" : ".mp4");

        SimpleConversionJob conversionJob = new SimpleConversionJob(new File(resultFilePath), file);
        logger.info("Launching video converter.");
        try {
            queuedConversionGauge.getAndIncrement();
            resultFilePath = CompletableFuture.supplyAsync(withMdc(new VideoController(conversionJob, activeConversions)), executorService).get();
            new File(resultFilePath).renameTo(file);
        } catch (InterruptedException | ExecutionException e){
            logger.error("Failure converting video.", e);
        } finally {
            queuedConversionGauge.getAndDecrement();
        }
    }

    private static <U> Supplier<U> withMdc(Supplier<U> supplier) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return (Supplier) () -> {
            MDC.setContextMap(mdc);
            return supplier.get();
        };
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
