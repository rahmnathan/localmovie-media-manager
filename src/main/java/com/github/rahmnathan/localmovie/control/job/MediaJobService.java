package com.github.rahmnathan.localmovie.control.job;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.event.MediaEventService;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.rahmnathan.localmovie.web.filter.CorrelationIdFilter.X_CORRELATION_ID;

@Slf4j
@Service
@AllArgsConstructor
public class MediaJobService {
    private final AtomicInteger queuedConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final AtomicInteger activeConversionGauge = Metrics.gauge("localmovie.conversions.active", new AtomicInteger(0));

    private static final String JOB_ID_METRIC_TAG = "jobId";

    private final MediaJobRepository mediaJobRepository;
    private final KubernetesService kubernetesService;
    private final MediaEventService mediaEventService;
    private final MeterRegistry meterRegistry;
    private final ServiceConfig serviceConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Number of concurrent video conversions allowed: {}", serviceConfig.getDirectoryMonitor().getConcurrentConversionLimit());
    }

    @Scheduled(fixedDelay = 30000L)
    @SchedulerLock(name = "scan-queued-jobs-lock")
    public void scanQueuedJobs() {
        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        int runningCount = mediaJobRepository.countAllByStatus(MediaJobStatus.RUNNING.name());
        int queuedCount = mediaJobRepository.countAllByStatus(MediaJobStatus.QUEUED.name());

        if (runningCount > 0 || queuedCount > 0) {
            log.info("Found {} active conversions and {} queued conversions.", runningCount, queuedCount);
        }

        // Launch new conversions, if applicable
        if (runningCount < serviceConfig.getDirectoryMonitor().getConcurrentConversionLimit() && queuedCount > 0) {

            int jobsToLaunch = Math.min(serviceConfig.getDirectoryMonitor().getConcurrentConversionLimit() - runningCount, queuedCount);
            List<MediaJob> mediaJobList = mediaJobRepository.findAllByStatusOrderByCreatedAsc(MediaJobStatus.QUEUED.name()).stream()
                    .limit(jobsToLaunch)
                    .toList();

            log.info("Launching {} new conversions.", mediaJobList.size());

            mediaJobList.forEach(mediaJob -> {
                mediaJob.setStatus(MediaJobStatus.RUNNING.name());
                mediaJobRepository.save(mediaJob);
            });

            for (MediaJob mediaJob : mediaJobList) {
                try {
                    launchVideoConverter(mediaJob);
                } catch (IOException e) {
                    log.error("Failed to launch video conversion for jobId: {}", mediaJob.getJobId(), e);
                    mediaJob.setStatus(MediaJobStatus.FAILED.name());
                    mediaJobRepository.save(mediaJob);
                }
            }
        }

        MDC.clear();
        meterRegistry.timer("localmovies.scan-queued-jobs").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    @Scheduled(fixedDelay = 30000L)
    @SchedulerLock(name = "update-job-status-lock")
    public void updateJobStatus() throws IOException {
        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        log.debug("Updating job status.");

        List<MediaJob> mediaJobs = mediaJobRepository.findAllByStatusInOrderByCreatedAsc(Set.of(MediaJobStatus.QUEUED.name(), MediaJobStatus.RUNNING.name()));

        for(MediaJob mediaJob : mediaJobs) {
            Optional<MediaJobStatus> jobStatus = kubernetesService.getJobStatus(mediaJob.getJobId());

            if(jobStatus.isEmpty()) {
                log.warn("Could not find status for kubernetes job with jobId: {}", mediaJob.getJobId());
                continue;
            }

            mediaJob.setStatus(jobStatus.get().name());

            if (jobStatus.get() == MediaJobStatus.SUCCEEDED) {
                log.info("Found completed job for input file: {}", mediaJob.getInputFile());
                kubernetesService.deleteJob(mediaJob.getJobId());
                File inputFile = new File(mediaJob.getInputFile());
                if(inputFile.exists()) {
                    Files.delete(inputFile.toPath());
                }
                mediaEventService.handleCreateEvent(new File(mediaJob.getOutputFile()));
            } else if (jobStatus.get() == MediaJobStatus.FAILED) {
                log.warn("Found failed job for input file: {}", mediaJob.getInputFile());
                kubernetesService.deleteJob(mediaJob.getJobId());
                File outputFile = new File(mediaJob.getOutputFile());
                if(outputFile.exists()) {
                    Files.delete(outputFile.toPath());
                }
            }
        }

        mediaJobRepository.saveAll(mediaJobs);

        int runningCount = mediaJobRepository.countAllByStatus(MediaJobStatus.RUNNING.name());
        activeConversionGauge.set(runningCount);
        int queuedCount = mediaJobRepository.countAllByStatus(MediaJobStatus.QUEUED.name());
        queuedConversionGauge.set(queuedCount);

        MDC.clear();
        meterRegistry.timer("localmovies.update-job-status").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    @Scheduled(fixedDelay = 30000L)
    @SchedulerLock(name = "record-etas-lock")
    public void extractAndRecordETAs() throws IOException {
        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        List<MediaJob> mediaJobs = mediaJobRepository.findAllByStatusOrderByCreatedAsc(MediaJobStatus.RUNNING.name());
        for (MediaJob mediaJob : mediaJobs) {
            kubernetesService.getETA(mediaJob.getJobId()).ifPresent(value -> meterRegistry.timer("conversion.time.left", List.of(Tag.of(JOB_ID_METRIC_TAG, mediaJob.getJobId()))).record(value));
        }

        MDC.clear();
        meterRegistry.timer("localmovies.record-etas").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    public void launchVideoConverter(MediaJob mediaJob) throws IOException {
        log.info("Launching video conversion for jobId: {}", mediaJob.getJobId());

        File inputFile = new File(mediaJob.getInputFile());
        File outputFile = new File(mediaJob.getOutputFile());

        if (Files.exists(outputFile.toPath())) {
            Files.delete(outputFile.toPath());
        }

        kubernetesService.launchVideoConverter(inputFile, outputFile);
    }
}
