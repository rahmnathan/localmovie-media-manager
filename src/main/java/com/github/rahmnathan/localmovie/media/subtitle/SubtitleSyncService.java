package com.github.rahmnathan.localmovie.media.subtitle;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.SubtitleSyncStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Service for synchronizing subtitles with video files using ffsubsync in a Kubernetes job.
 *
 * ffsubsync uses speech recognition to detect when dialogue occurs in the video
 * and aligns subtitle timestamps to match. This handles cases where subtitles
 * were made for a different video release (different intro, credits, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleSyncService {

    private static final String JOB_NAME_LABEL = "job-name";
    private static final String APP_LABEL = "app";
    private static final String SUBTITLE_SYNC_APP = "subtitle-sync";
    private static final String SUBTITLE_SYNC_TEMP_PREFIX = ".subtitle-sync-";
    private static final int SYNC_JOB_TTL_SECONDS = 3600;

    private final ServiceConfig serviceConfig;
    private final MeterRegistry meterRegistry;
    private final KubernetesClient kubernetesClient;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupStaleSyncArtifacts() {
        if (serviceConfig.getMediaPaths() == null) {
            return;
        }

        ServiceConfig.OpenSubtitlesConfig opensubtitles = serviceConfig.getOpensubtitles();
        if (opensubtitles == null) {
            return;
        }

        Instant staleBefore = Instant.now().minus(Duration.ofSeconds(
                opensubtitles.getSyncTimeoutSeconds() + 600L));
        serviceConfig.getMediaPaths().forEach(mediaPath -> cleanupStaleSyncArtifacts(Paths.get(mediaPath), staleBefore));
    }

    /**
     * Check if sync is enabled.
     */
    public boolean isSyncEnabled() {
        ServiceConfig.OpenSubtitlesConfig config = serviceConfig.getOpensubtitles();
        return config != null && config.isSyncEnabled();
    }

    /**
     * Launch a subtitle sync job asynchronously.
     *
     * @param videoPath Absolute path to the video file
     * @param subtitleContent The VTT subtitle content to sync
     * @return SyncJobInfo with job name and temp directory, or empty if launch failed
     */
    public Optional<SyncJobInfo> launchSyncJob(String videoPath, String subtitleContent) {
        if (videoPath == null || subtitleContent == null) {
            return Optional.empty();
        }

        Path tempDir = null;
        try {
            Path videoFile = Paths.get(videoPath);
            if (videoFile.getParent() == null) {
                log.warn("Cannot sync subtitle because video path has no parent directory: {}", videoPath);
                return Optional.empty();
            }

            String jobName = "subtitle-sync-" + UUID.randomUUID().toString().substring(0, 8);
            var dirPermissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));
            tempDir = Files.createTempDirectory(videoFile.getParent(), SUBTITLE_SYNC_TEMP_PREFIX + jobName + "-", dirPermissions);
            Path inputFile = tempDir.resolve("input.vtt");
            Path outputFile = tempDir.resolve("output.vtt");

            var filePermissions = PosixFilePermissions.fromString("rw-rw-rw-");
            Files.writeString(inputFile, subtitleContent, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(inputFile, filePermissions);
            // Pre-create output file with write permissions for the sync job
            Files.createFile(outputFile);
            Files.setPosixFilePermissions(outputFile, filePermissions);

            String namespace = getNamespace();
            createSyncJob(kubernetesClient, namespace, jobName, videoFile, inputFile, outputFile);

            log.info("Launched subtitle sync job {} for video: {}", jobName, videoPath);
            return Optional.of(new SyncJobInfo(jobName, tempDir.toString()));

        } catch (Exception e) {
            log.error("Failed to launch subtitle sync job for video: {}", videoPath, e);
            meterRegistry.counter("localmovies.subtitle-sync.launch-failed").increment();
            // Clean up temp directory if it was created before failure
            if (tempDir != null) {
                cleanup(tempDir);
            }
            return Optional.empty();
        }
    }

    /**
     * Check the status of a sync job.
     *
     * @param jobName The Kubernetes job name
     * @return The current sync status
     */
    public SubtitleSyncStatus checkSyncJobStatus(String jobName) {
        try {
            String namespace = getNamespace();
            Job job = kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();

            if (job == null) {
                log.warn("Sync job not found: {}", jobName);
                return SubtitleSyncStatus.FAILED;
            }

            JobStatus status = job.getStatus();
            if (status == null) {
                return SubtitleSyncStatus.RUNNING;
            }

            if (status.getSucceeded() != null && status.getSucceeded() > 0) {
                return SubtitleSyncStatus.SUCCEEDED;
            }

            if (status.getFailed() != null && status.getFailed() > 0) {
                return SubtitleSyncStatus.FAILED;
            }

            return SubtitleSyncStatus.RUNNING;

        } catch (Exception e) {
            log.error("Failed to check sync job status for: {}", jobName, e);
            return SubtitleSyncStatus.FAILED;
        }
    }

    /**
     * Collect the result of a completed sync job.
     *
     * @param jobName The Kubernetes job name
     * @param tempDir The temp directory containing input/output files
     * @return The synced subtitle content, or empty if collection failed
     */
    public Optional<String> collectSyncResult(String jobName, String tempDir) {
        Path tempDirPath = Paths.get(tempDir);
        Path outputFile = tempDirPath.resolve("output.vtt");

        try {
            if (!Files.exists(outputFile)) {
                log.warn("Sync output file does not exist: {}", outputFile);
                meterRegistry.counter("localmovies.subtitle-sync.failed").increment();
                return Optional.empty();
            }

            String syncedContent = Files.readString(outputFile, StandardCharsets.UTF_8);
            meterRegistry.counter("localmovies.subtitle-sync.succeeded").increment();
            log.info("Successfully collected synced subtitle from job: {}", jobName);
            return Optional.of(syncedContent);

        } catch (Exception e) {
            log.error("Failed to collect sync result for job: {}", jobName, e);
            meterRegistry.counter("localmovies.subtitle-sync.failed").increment();
            return Optional.empty();
        }
    }

    /**
     * Clean up after a sync job (delete K8s job and temp directory).
     *
     * @param jobName The Kubernetes job name
     * @param tempDir The temp directory to clean up
     */
    public void cleanupSyncJob(String jobName, String tempDir) {
        // Delete Kubernetes job
        try {
            String namespace = getNamespace();
            kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).delete();
            log.debug("Deleted sync job: {}", jobName);
        } catch (Exception e) {
            log.warn("Failed to delete sync job: {}", jobName, e);
        }

        // Clean up temp directory
        if (tempDir != null) {
            cleanup(Paths.get(tempDir));
        }
    }

    /**
     * Get pod logs for a sync job (useful for debugging failures).
     */
    public String getSyncJobLog(String jobName) {
        try {
            String namespace = getNamespace();
            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withLabel(JOB_NAME_LABEL, jobName)
                    .list()
                    .getItems()
                    .stream()
                    .findAny()
                    .map(pod -> kubernetesClient.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).getLog())
                    .orElse("");
        } catch (Exception e) {
            log.warn("Failed to get pod log for job: {}", jobName, e);
            return "";
        }
    }

    private void createSyncJob(KubernetesClient client, String namespace, String jobName,
                               Path videoFile, Path inputFile, Path outputFile) throws IOException {
        Optional<Pod> localmoviesPodOptional = kubernetesClient.pods().inNamespace(namespace).list().getItems().stream()
                .filter(pod -> "localmovies".equalsIgnoreCase(labelValue(pod, APP_LABEL)))
                .findAny();

        if (localmoviesPodOptional.isEmpty()) {
            throw new IOException("Could not find localmovies pod to copy volume mounts from");
        }

        List<Volume> volumes = localmoviesPodOptional.get().getSpec().getVolumes().stream()
                .filter(volume -> volume.getName().startsWith("media"))
                .toList();

        List<VolumeMount> volumeMounts = localmoviesPodOptional.get().getSpec().getContainers().stream()
                .filter(container -> "localmovies".equalsIgnoreCase(container.getName()))
                .findAny()
                .orElseThrow(() -> new IOException("Could not find localmovies container to copy volume mounts from"))
                .getVolumeMounts().stream()
                .filter(volumeMount -> volumeMount.getName().startsWith("media"))
                .toList();

        ResourceRequirements resources = new ResourceRequirements(
                new ArrayList<>(),
                Map.of("cpu", Quantity.parse("2"),
                        "memory", Quantity.parse("2Gi")),
                Map.of("cpu", Quantity.parse("500m"),
                        "memory", Quantity.parse("512Mi"))
        );

        Job job = new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                    .withName(jobName)
                    .withLabels(Map.of(APP_LABEL, SUBTITLE_SYNC_APP))
                .endMetadata()
                .withNewSpec()
                    .withBackoffLimit(0)
                    .withTtlSecondsAfterFinished(SYNC_JOB_TTL_SECONDS)
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(Map.of(APP_LABEL, SUBTITLE_SYNC_APP))
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(jobName)
                                .withImage(serviceConfig.getOpensubtitles().getSyncImage())
                                .withCommand("ffsubsync")
                                .withArgs(
                                        videoFile.toString(),
                                        "-i", inputFile.toString(),
                                        "-o", outputFile.toString(),
                                        "--max-offset-seconds", String.valueOf(serviceConfig.getOpensubtitles().getSyncMaxOffsetSeconds()))
                                .withVolumeMounts(volumeMounts)
                                .withResources(resources)
                            .endContainer()
                            .withVolumes(volumes)
                            .withRestartPolicy("Never")
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        log.info("Creating subtitle sync job with name: {}", jobName);
        kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(job).create();
    }

    private String getNamespace() throws IOException {
        Path namespaceFile = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (namespaceFile.toFile().exists()) {
            return Files.readString(namespaceFile).trim();
        }

        return "localmovies";
    }

    private String labelValue(Pod pod, String label) {
        if (pod.getMetadata() == null || pod.getMetadata().getLabels() == null) {
            return null;
        }

        return pod.getMetadata().getLabels().get(label);
    }

    public void cleanup(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                try (Stream<Path> paths = Files.walk(tempDir)) {
                    paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.debug("Failed to delete temp file: {}", path);
                            }
                        });
                }
            }
        } catch (IOException e) {
            log.debug("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    private void cleanupStaleSyncArtifacts(Path mediaPath, Instant staleBefore) {
        if (!Files.exists(mediaPath)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(mediaPath)) {
            List<Path> staleTempDirs = paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName() != null)
                    .filter(path -> path.getFileName().toString().startsWith(SUBTITLE_SYNC_TEMP_PREFIX))
                    .filter(path -> isOlderThan(path, staleBefore))
                    .toList();
            staleTempDirs.forEach(this::cleanup);
        } catch (IOException e) {
            log.warn("Failed to scan stale subtitle sync artifacts under {}", mediaPath, e);
        }
    }

    private boolean isOlderThan(Path path, Instant staleBefore) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(staleBefore);
        } catch (IOException e) {
            log.debug("Failed to read last modified time for {}", path, e);
            return false;
        }
    }

    /**
     * Information about a launched sync job.
     */
    public record SyncJobInfo(String jobName, String tempDir) {}
}
