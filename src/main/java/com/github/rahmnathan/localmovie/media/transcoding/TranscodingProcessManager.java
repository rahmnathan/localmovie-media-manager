package com.github.rahmnathan.localmovie.media.transcoding;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class TranscodingProcessManager {
    private final Map<String, ProcessInfo> activeProcesses = new ConcurrentHashMap<>();
    private final Semaphore concurrencyLimit;
    private final MeterRegistry registry;
    private final int processTimeoutMinutes = 30;

    public TranscodingProcessManager(ServiceConfig serviceConfig, MeterRegistry registry) {
        int maxConcurrent = serviceConfig.getTranscoding().getMaxConcurrentSessions();
        this.concurrencyLimit = new Semaphore(maxConcurrent);
        this.registry = registry;

        // Register gauge for active transcoding sessions
        Gauge.builder("localmovies.transcoding.active", activeProcesses, Map::size)
            .description("Number of active transcoding sessions")
            .register(registry);

        log.info("TranscodingProcessManager initialized with max concurrent sessions: {}", maxConcurrent);
    }

    public Process acquireTranscodingProcess(String sessionId, Supplier<Process> processStarter) throws InterruptedException {
        boolean acquired = concurrencyLimit.tryAcquire(5, TimeUnit.SECONDS);
        if (!acquired) {
            log.warn("Failed to acquire transcoding slot for session: {}", sessionId);
            throw new IllegalStateException("Transcoding capacity limit reached");
        }

        try {
            Process process = processStarter.get();
            activeProcesses.put(sessionId, new ProcessInfo(process, System.currentTimeMillis()));

            registry.counter("localmovies.transcoding.started").increment();
            log.info("Acquired transcoding process for session: {} (active: {})", sessionId, activeProcesses.size());

            return process;
        } catch (Exception e) {
            // If we fail to start, release the permit
            concurrencyLimit.release();
            registry.counter("localmovies.transcoding.failed").increment();
            throw e;
        }
    }

    public void releaseProcess(String sessionId) {
        ProcessInfo info = activeProcesses.remove(sessionId);
        if (info != null) {
            Process process = info.process();

            if (process.isAlive()) {
                log.info("Terminating transcoding process for session: {}", sessionId);
                process.destroyForcibly();
            }

            concurrencyLimit.release();

            long durationMs = System.currentTimeMillis() - info.startTime();
            registry.timer("localmovies.transcoding.duration").record(durationMs, TimeUnit.MILLISECONDS);
            registry.counter("localmovies.transcoding.completed").increment();

            log.info("Released transcoding process for session: {} (duration: {}ms, active: {})",
                sessionId, durationMs, activeProcesses.size());
        }
    }

    @Scheduled(fixedDelay = 60000) // Every minute
    public void monitorProcesses() {
        long now = System.currentTimeMillis();
        long timeoutMs = TimeUnit.MINUTES.toMillis(processTimeoutMinutes);

        activeProcesses.forEach((sessionId, info) -> {
            // Check if process is still alive
            if (!info.process().isAlive()) {
                log.warn("Detected dead transcoding process for session: {}, cleaning up", sessionId);
                releaseProcess(sessionId);
                return;
            }

            // Check for timeout
            long runningTime = now - info.startTime();
            if (runningTime > timeoutMs) {
                log.warn("Transcoding process timed out for session: {} (running: {}ms), terminating",
                    sessionId, runningTime);
                releaseProcess(sessionId);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up all active transcoding processes (count: {})", activeProcesses.size());

        activeProcesses.forEach((sessionId, info) -> {
            Process process = info.process();
            if (process.isAlive()) {
                log.info("Terminating transcoding process for session: {}", sessionId);
                process.destroyForcibly();
            }
        });

        activeProcesses.clear();
    }

    public int getActiveSessionCount() {
        return activeProcesses.size();
    }

    private record ProcessInfo(Process process, long startTime) {}
}
