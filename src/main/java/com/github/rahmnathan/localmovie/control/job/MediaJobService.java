package com.github.rahmnathan.localmovie.control.job;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.event.MediaEventService;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class MediaJobService {
    private final AtomicInteger queuedConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final AtomicInteger activeConversionGauge = Metrics.gauge("localmovie.conversions.active", new AtomicInteger(0));

    private static final Set<String> ACTIVE_STATUSES = Set.of(MediaJobStatus.QUEUED.name(), MediaJobStatus.RUNNING.name());

    private static final String HANDBRAKE_PRESET = "Chromecast 1080p60 Surround";

    private final ServiceConfig.MediaEventMonitorConfig eventMonitorConfig;
    private final MediaJobRepository mediaJobRepository;
    private final KubernetesService kubernetesService;
    private final MediaEventService mediaEventService;
    private final MeterRegistry meterRegistry;

    public MediaJobService(ServiceConfig serviceConfig, MediaJobRepository mediaJobRepository, MediaEventService mediaEventService,
                           KubernetesService kubernetesService, MeterRegistry registry) {
        eventMonitorConfig = serviceConfig.getDirectoryMonitor();
        log.info("Number of concurrent video conversions allowed: {}", eventMonitorConfig.getConcurrentConversionLimit());
        this.mediaJobRepository = mediaJobRepository;
        this.kubernetesService = kubernetesService;
        this.mediaEventService = mediaEventService;
        this.meterRegistry = registry;
    }

    @Scheduled(fixedDelay = 120000L)
    public void scanQueuedJobs() throws Exception {
        long startTime = System.currentTimeMillis();

        log.info("Scanning for video conversions.");

        // Get DB state
        int runningCount = mediaJobRepository.countAllByStatus(MediaJobStatus.RUNNING.name());
        int queuedCount = mediaJobRepository.countAllByStatus(MediaJobStatus.QUEUED.name());

        log.info("Found {} active conversions and {} queued conversions.", runningCount, queuedCount);

        // Launch new conversions, if applicable
        if (runningCount < eventMonitorConfig.getConcurrentConversionLimit() && queuedCount > 0) {
            int jobsToLaunch = Math.min(eventMonitorConfig.getConcurrentConversionLimit() - runningCount, queuedCount);

            List<MediaJob> mediaJobList = mediaJobRepository.findAllByStatusOrderByCreatedAsc(MediaJobStatus.QUEUED.name()).stream()
                    .limit(jobsToLaunch)
                    .toList();

            log.info("Launching {} new conversions.", mediaJobList.size());

            mediaJobList.forEach(mediaJob -> {
                mediaJob.setStatus(MediaJobStatus.RUNNING.name());
                mediaJobRepository.save(mediaJob);
            });

            for (MediaJob mediaJob : mediaJobList) {
                Optional<MediaJobStatus> jobStatus = kubernetesService.getJobStatus(mediaJob.getJobId());

                if (jobStatus.isPresent() && (jobStatus.get() == MediaJobStatus.SUCCEEDED || jobStatus.get() == MediaJobStatus.RUNNING)) {
                    log.info("Job with status {} already exists for input file: {}", jobStatus, formatPath(mediaJob.getInputFile()));
                    continue;
                }

                launchVideoConverter(mediaJob);
            }
        }

        meterRegistry.timer("localmovies.scan-queued-jobs").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    @Timed
    @Scheduled(fixedDelay = 10000L)
    public void updateJobStatus() throws Exception {
        long startTime = System.currentTimeMillis();

        log.info("Updating job status.");

        kubernetesService.extractAndRecordETAs();

        List<MediaJob> mediaJobs = mediaJobRepository.findAllByStatusInOrderByCreatedAsc(Set.of(MediaJobStatus.QUEUED.name(), MediaJobStatus.RUNNING.name()));

        for(MediaJob mediaJob : mediaJobs) {
            Optional<MediaJobStatus> jobStatus = kubernetesService.getJobStatus(mediaJob.getJobId());

            if(jobStatus.isEmpty()) {
                continue;
            }

            mediaJob.setStatus(jobStatus.get().name());

            if (jobStatus.get() == MediaJobStatus.SUCCEEDED) {
                log.info("Found completed job for input file: {}", mediaJob.getInputFile());
                kubernetesService.deleteJob(mediaJob.getJobId());
                new File(mediaJob.getInputFile()).delete();
                mediaEventService.handleCreateEvent(new File(mediaJob.getOutputFile()));
            } else if (jobStatus.get() == MediaJobStatus.FAILED) {
                log.warn("Found failed job for input file: {}", mediaJob.getInputFile());
                kubernetesService.deleteJob(mediaJob.getJobId());
                new File(mediaJob.getOutputFile()).delete();
            }
        }

        mediaJobRepository.saveAll(mediaJobs);

        int runningCount = mediaJobRepository.countAllByStatus(MediaJobStatus.RUNNING.name());
        activeConversionGauge.set(runningCount);
        int queuedCount = mediaJobRepository.countAllByStatus(MediaJobStatus.QUEUED.name());
        queuedConversionGauge.set(queuedCount);

        meterRegistry.timer("localmovies.update-job-status").record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    private String formatPath(String path) {
        return path.split(File.separator + "LocalMedia" + File.separator)[1].replaceAll("[^A-Za-z0-9]", "-");
    }

    public boolean isActiveConversion(File file) {
        return mediaJobRepository.existsByOutputFileAndStatusIn(file.toString(), ACTIVE_STATUSES) ||
                mediaJobRepository.existsByInputFileAndStatusIn(file.toString(), ACTIVE_STATUSES);
    }

    public void createConversionJob(File file) {
        log.info("Creating video conversion job.");

        String inputPath = file.toString();
        String resultFilePath = inputPath.substring(0, inputPath.lastIndexOf('.')) + (inputPath.endsWith(".mp4") ? ".mkv" : ".mp4");

        MediaJob mediaJob = MediaJob.builder()
                .inputFile(inputPath)
                .outputFile(resultFilePath)
                .jobId(formatPath(inputPath))
                .handbrakePreset(HANDBRAKE_PRESET)
                .status(MediaJobStatus.QUEUED.name())
                .build();

        mediaJobRepository.save(mediaJob);

        log.info("Video conversion job created.");
    }

    public void launchVideoConverter(MediaJob mediaJob) {
        log.info("Launching video converter.");

        File inputFile = new File(mediaJob.getInputFile());
        File outputFile = new File(mediaJob.getOutputFile());

        try {
            if (Files.exists(outputFile.toPath())) {
                Files.delete(outputFile.toPath());
            }

            kubernetesService.launchVideoConverter(inputFile, outputFile);

            log.info("Created job successfully.");
        } catch (IOException e) {
            log.error("Failed to create video conversion job.", e);
        }
    }
}
