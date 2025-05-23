package com.github.rahmnathan.localmovie.media.event.files;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "service.directoryMonitor.enabled", havingValue = "true")
public class DirectoryMonitor {
    private final Set<DirectoryMonitorObserver> observers;
    private final ServiceConfig config;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Starting Recursive Watcher Service with {} observers.", observers.size());

        FileAlterationMonitor monitor = new FileAlterationMonitor();
        FileAlterationListener listener = new DirectoryMonitorListener(monitor, observers);

        config.getMediaPaths().stream().map(Paths::get).forEach(p -> {
            try {
                log.info("registering {} in watcher service", p);
                FileAlterationObserver observer = FileAlterationObserver.builder()
                        .setPath(p)
                        .get();
                observer.addListener(listener);
                monitor.addObserver(observer);
            } catch (Exception e) {
                log.error("Failed to register {} in watcher service", p, e);
            }
        });

        try {
            monitor.start();
        } catch (Exception e) {
            // TODO - Figure out why UTF-8 isn't working for special characters anymore
            log.error("Directory monitor failed to start.", e);
//            throw new RuntimeException(e);
        }
    }
}