package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.control.event.MediaEventService;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import com.github.rahmnathan.video.cast.handbrake.boundary.VideoConverterHandbrake;
import com.github.rahmnathan.video.converter.data.HandbrakePreset;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class MediaConversionService {
    private final AtomicInteger queuedConversionGauge = Metrics.gauge("localmovie.conversions.queued", new AtomicInteger(0));
    private final AtomicInteger activeConversionGauge = Metrics.gauge("localmovie.conversions.active", new AtomicInteger(0));

    private final Set<String> ACTIVE_STATUSES = Set.of(MediaJobStatus.QUEUED.name(), MediaJobStatus.RUNNING.name());

    private final ServiceConfig.MediaEventMonitorConfig eventMonitorConfig;
    private final MediaJobRepository mediaJobRepository;
    private final MediaEventService mediaEventService;

    public MediaConversionService(ServiceConfig serviceConfig, MediaJobRepository mediaJobRepository, MediaEventService mediaEventService) {
        eventMonitorConfig = serviceConfig.getDirectoryMonitor();
        log.info("Number of concurrent video conversions allowed: {}", eventMonitorConfig.getConcurrentConversionLimit());
        this.mediaJobRepository = mediaJobRepository;
        this.mediaEventService = mediaEventService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void scanConversions() throws Exception {
        log.info("Scanning for video conversions.");

        String namespace = Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace"));

        // Update status of jobs
        updateJobStatus(namespace);

        // Update metrics
        int runningCount = mediaJobRepository.countAllByStatus(MediaJobStatus.RUNNING.name());
        int queuedCount = mediaJobRepository.countAllByStatus(MediaJobStatus.QUEUED.name());

        log.info("Found {} active conversions and {} queued conversions.", runningCount, queuedCount);

        // Launch new conversions, if applicable
        if (runningCount < eventMonitorConfig.getConcurrentConversionLimit() && queuedCount > 0) {
            int jobsToLaunch = queuedCount - runningCount;
            jobsToLaunch = Math.min(jobsToLaunch, eventMonitorConfig.getConcurrentConversionLimit());

            List<MediaJob> mediaJobList = mediaJobRepository.findAllByStatusOrderByCreatedAsc(MediaJobStatus.QUEUED.name()).stream()
                    .limit(jobsToLaunch)
                    .toList();

            log.info("Launching {} new conversions.", mediaJobList.size());

            mediaJobList.forEach(mediaJob -> {
                mediaJob.setStatus(MediaJobStatus.RUNNING.name());
                mediaJobRepository.save(mediaJob);
            });

            try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {
                for (MediaJob mediaJob : mediaJobList) {
                    JobList jobList = client.batch().v1().jobs().inNamespace(namespace).withLabel("inputPath", formatPath(mediaJob.getInputFile())).list();

                    if (jobList.getItems().stream().anyMatch(job -> (job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded() > 0) ||
                                                                     job.getStatus().getActive() != null && job.getStatus().getActive() > 0)) {
                        log.info("Job already succeeded (or active) for input file: {}", formatPath(mediaJob.getInputFile()));
                        continue;
                    }

                    launchVideoConverter(mediaJob);
                }
            }

            updateJobStatus(namespace);
        }
    }

    private void updateJobStatus(String namespace) {
        try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {

            List<Job> jobList = client.batch().v1().jobs().inNamespace(namespace).withLabel("app", "handbrake").list().getItems();

            for (Job job : jobList) {
                MediaJob mediaJob = mediaJobRepository.findByJobId(job.getMetadata().getLabels().get("jobId"));
                if (mediaJob == null) {
                    log.warn("Job found for input file that does not exist in database: {}", job.getMetadata().getLabels().get("jobId"));
                    continue;
                }

                if (job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded() > 0 &&
                        mediaJob.getCreated().isBefore(LocalDateTime.now().minus(7, ChronoUnit.DAYS))) {
                    log.info("Deleting completed job for input file: {}", mediaJob.getInputFile());
                    mediaJobRepository.delete(mediaJob);
                    continue;
                } else if (job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded() > 0) {
                    if(!MediaJobStatus.SUCCEEDED.name().equalsIgnoreCase(mediaJob.getStatus())){
                        completeJob(mediaJob);
                    }
                } else if (job.getStatus().getFailed() != null && job.getStatus().getFailed() > 0) {
                    mediaJob.setStatus(MediaJobStatus.FAILED.name());
                } else if (job.getStatus().getActive() != null && job.getStatus().getActive() > 0) {
                    mediaJob.setStatus(MediaJobStatus.RUNNING.name());
                } else {
                    mediaJob.setStatus(MediaJobStatus.UNKNOWN.name());
                }

                mediaJobRepository.save(mediaJob);
            }
        }

        int runningCount = mediaJobRepository.countAllByStatus(MediaJobStatus.RUNNING.name());
        activeConversionGauge.set(runningCount);
        int queuedCount = mediaJobRepository.countAllByStatus(MediaJobStatus.QUEUED.name());
        queuedConversionGauge.set(queuedCount);
    }

    public void completeJob(MediaJob mediaJob) {
        log.info("Video conversion complete.");
        new File(mediaJob.getInputFile()).delete();
        new File(mediaJob.getOutputFile()).renameTo(new File(mediaJob.getInputFile()));
        mediaJob.setStatus(MediaJobStatus.SUCCEEDED.name());
        mediaJobRepository.save(mediaJob);
        mediaEventService.handleCreateEvent(new File(mediaJob.getInputFile()));
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
                .handbrakePreset(HandbrakePreset.CHROMECAST_1080p_60fps.getValue())
                .status(MediaJobStatus.QUEUED.name())
                .build();

        mediaJobRepository.save(mediaJob);

        log.info("Video conversion job created.");
    }

    public void launchVideoConverter(MediaJob mediaJob) {
        SimpleConversionJob conversionJob = SimpleConversionJob.builder()
                .handbrakePreset(HandbrakePreset.CHROMECAST_1080p_60fps.getValue())
                .inputFile(new File(mediaJob.getInputFile()))
                .outputFile(new File(mediaJob.getOutputFile()))
                .build();

        log.info("Launching video converter.");

        CompletableFuture.supplyAsync(new VideoConverterHandbrake(conversionJob));
    }
}
