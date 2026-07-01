package com.github.rahmnathan.localmovie.media.job;

import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing Kubernetes jobs for video conversion.
 * Handles job status checking, deletion, and ETA extraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesService {

    private final KubernetesClient kubernetesClient;

    private static final String JOB_NAME = "job-name";
    private static final String JOB_ID_LABEL = "jobId";

    private static final Pattern HANDBRAKE_ETA_PATTERN = Pattern.compile("(\\d{2})h(\\d{2})m(?:(\\d{2})s)?");
    private static final Pattern FFMPEG_DURATION_PATTERN = Pattern.compile("Duration:\\s*(\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)");
    private static final Pattern FFMPEG_PROGRESS_TIME_PATTERN = Pattern.compile("(?m)^out_time=(\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)");
    private static final Pattern FFMPEG_STATS_TIME_PATTERN = Pattern.compile("time=(\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)");
    private static final Pattern FFMPEG_SPEED_PATTERN = Pattern.compile("(?m)(?:^|\\s)speed=\\s*([0-9.]+)x");

    public Optional<MediaJobStatus> getJobStatus(String jobId) throws IOException {
        Optional<JobStatus> jobStatusOptional = kubernetesClient.batch().v1().jobs().inNamespace(getNamespace())
                .withLabel(JOB_ID_LABEL, jobId).list().getItems()
                .stream()
                .findFirst()
                .map(Job::getStatus);

        if (jobStatusOptional.isPresent()) {

            JobStatus jobStatus = jobStatusOptional.get();

            if (jobStatus.getSucceeded() != null && jobStatus.getSucceeded() > 0) {
                return Optional.of(MediaJobStatus.SUCCEEDED);
            } else if (jobStatus.getFailed() != null && jobStatus.getFailed() > 0) {
                return Optional.of(MediaJobStatus.FAILED);
            } else if (jobStatus.getActive() != null && jobStatus.getActive() > 0) {
                return Optional.of(MediaJobStatus.RUNNING);
            }
        }

        return Optional.empty();
    }

    public void deleteJob(String jobId) throws IOException {
        Optional<Job> jobOptional = kubernetesClient.batch().v1().jobs()
                .inNamespace(getNamespace())
                .withLabel(JOB_ID_LABEL, jobId).list().getItems().stream()
                .findAny();

        if(jobOptional.isPresent()) {
            kubernetesClient.batch().v1().jobs().inNamespace(getNamespace()).resource(jobOptional.get()).delete();
        } else {
            log.warn("No kubernetes job found for jobId: {}", jobId);
        }
    }

    public Optional<Duration> getETA(String jobId) throws IOException {
        return getETA(jobId, null);
    }

    public Optional<Duration> getETA(String jobId, Long durationSeconds) throws IOException {
        String namespace = getNamespace();

        // Lookup job by jobId
        List<Job> jobList = kubernetesClient.batch().v1().jobs()
                .inNamespace(namespace)
                .withLabel(JOB_ID_LABEL, jobId)
                .list()
                .getItems();

        if (jobList.isEmpty()) {
            log.warn("No Kubernetes jobs for jobId: {}", jobId);
            return Optional.empty();
        }

        String jobName = jobList.getFirst().getMetadata().getName();

        // Find running pod associated with job
        Optional<Pod> podOptional = kubernetesClient.pods()
                .inNamespace(namespace)
                .withLabel(JOB_NAME, jobName)
                .list().getItems().stream()
                .filter(pod -> "running".equalsIgnoreCase(Optional.ofNullable(pod.getStatus())
                        .map(status -> status.getPhase())
                        .orElse(null)))
                .findAny();

        if (podOptional.isEmpty()) {
            log.warn("No running pod for jobId: {}", jobId);
            return Optional.empty();
        }

        String podName = podOptional.get().getMetadata().getName();

        String podLog = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .getLog();

        return parseETA(podLog, durationSeconds);
    }

    Optional<Duration> parseETA(String podLog, Long durationSeconds) {
        Optional<Duration> handbrakeEta = parseHandbrakeETA(podLog);
        if (handbrakeEta.isPresent()) {
            return handbrakeEta;
        }

        Optional<Duration> totalDuration = Optional.ofNullable(durationSeconds)
                .filter(value -> value > 0)
                .map(Duration::ofSeconds)
                .or(() -> parseFfmpegTotalDuration(podLog));

        if (totalDuration.isEmpty()) {
            return Optional.empty();
        }

        Optional<Duration> encodedDuration = parseFfmpegEncodedDuration(podLog);
        Optional<Double> speed = parseFfmpegSpeed(podLog);
        if (encodedDuration.isEmpty() || speed.isEmpty() || speed.get() <= 0) {
            return Optional.empty();
        }

        long remainingMediaSeconds = totalDuration.get().toSeconds() - encodedDuration.get().toSeconds();
        if (remainingMediaSeconds <= 0) {
            return Optional.of(Duration.ZERO);
        }

        return Optional.of(Duration.ofSeconds(Math.round(remainingMediaSeconds / speed.get())));
    }

    private Optional<Duration> parseHandbrakeETA(String podLog) {
        // Example pod log: "ETA 01h25m15s)"
        Matcher etaMatcher = HANDBRAKE_ETA_PATTERN.matcher(podLog);
        if (etaMatcher.find()) {
            String eta = etaMatcher.group();
            log.debug("Found Handbrake ETA in logs ({}). Recording metric.", eta);

            long hours = Long.parseLong(etaMatcher.group(1));
            long minutes = Long.parseLong(etaMatcher.group(2));
            long seconds = etaMatcher.group(3) == null ? 0 : Long.parseLong(etaMatcher.group(3));

            return Optional.of(Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds));
        }

        return Optional.empty();
    }

    private Optional<Duration> parseFfmpegEncodedDuration(String podLog) {
        Matcher progressMatcher = FFMPEG_PROGRESS_TIME_PATTERN.matcher(podLog);
        String encodedTime = null;
        while (progressMatcher.find()) {
            encodedTime = progressMatcher.group(1);
        }

        if (encodedTime == null) {
            Matcher statsMatcher = FFMPEG_STATS_TIME_PATTERN.matcher(podLog);
            while (statsMatcher.find()) {
                encodedTime = statsMatcher.group(1);
            }
        }

        return encodedTime == null ? Optional.empty() : Optional.of(parseTimestamp(encodedTime));
    }

    private Optional<Duration> parseFfmpegTotalDuration(String podLog) {
        Matcher durationMatcher = FFMPEG_DURATION_PATTERN.matcher(podLog);
        return durationMatcher.find() ? Optional.of(parseTimestamp(durationMatcher.group(1))) : Optional.empty();
    }

    private Optional<Double> parseFfmpegSpeed(String podLog) {
        Matcher speedMatcher = FFMPEG_SPEED_PATTERN.matcher(podLog);
        Double speed = null;
        while (speedMatcher.find()) {
            speed = Double.parseDouble(speedMatcher.group(1));
        }

        return Optional.ofNullable(speed);
    }

    private Duration parseTimestamp(String timestamp) {
        String[] timeAndFraction = timestamp.split("\\.", 2);
        String[] time = timeAndFraction[0].split(":");

        Duration duration = Duration.ofHours(Long.parseLong(time[0]))
                .plusMinutes(Long.parseLong(time[1]))
                .plusSeconds(Long.parseLong(time[2]));

        if (timeAndFraction.length == 2) {
            String nanos = (timeAndFraction[1] + "000000000").substring(0, 9);
            duration = duration.plusNanos(Long.parseLong(nanos));
        }

        return duration;
    }

    private String getNamespace() throws IOException {
        Path namespaceFile = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (namespaceFile.toFile().exists()) {
            return Files.readString(namespaceFile).trim();
        }

        return "localmovies";
    }
}
