package com.github.rahmnathan.localmovie.media.job;

import com.github.rahmnathan.localmovie.data.MediaPath;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * FFmpeg-based video converter with EBU R128 loudness normalization.
 *
 * Produces Chromecast-compatible output:
 * - Video: H.264 High Profile Level 4.1 (compatible with all Chromecast generations)
 * - Audio: AAC stereo with loudnorm filter for consistent volume levels
 * - Container: MP4 with faststart for streaming
 */
@Slf4j
@Component("ffmpegVideoConverter")
public class FfmpegVideoConverter implements VideoConverter {

    private static final String JOB_ID_LABEL = "jobId";

    @Override
    public void launchVideoConverter(File inputFile, File outputFile) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            String namespace = getNamespace();

            Optional<Pod> localmoviesPodOptional = client.pods().inNamespace(namespace).list().getItems().stream()
                    .filter(pod -> "localmovies".equalsIgnoreCase(labelValue(pod, "app")))
                    .findAny();

            if (localmoviesPodOptional.isEmpty()) {
                throw new IOException("Could not find localmovies pod to copy volume mounts from");
            }

            String podName = "ffmpeg-" + UUID.randomUUID();

            log.info("Creating FFmpeg job with name: {}", podName);

            // FFmpeg arguments for Chromecast-compatible output with loudness normalization
            // Video: H.264 High Profile Level 4.1 (works on ALL Chromecasts)
            // Audio: AAC stereo with EBU R128 loudness normalization
            List<String> args = List.of(
                    "-nostats",
                    "-stats_period", "30",
                    "-progress", "pipe:1",
                    "-i", inputFile.getAbsolutePath(),
                    // Video encoding - H.264 High Profile Level 4.1 for maximum compatibility
                    "-c:v", "libx264",
                    "-profile:v", "high",
                    "-level:v", "4.1",
                    "-pix_fmt", "yuv420p",
                    "-crf", "18",
                    "-preset", "medium",
                    // Audio encoding with loudness normalization
                    // loudnorm: EBU R128 standard for broadcast loudness
                    // I=-16: Target integrated loudness (-16 LUFS, good for streaming)
                    // TP=-1.5: True peak limit to prevent clipping
                    // LRA=7: Loudness range - lower value = more compression of dynamic range
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-ac", "2",
                    "-af", "loudnorm=I=-16:TP=-1.5:LRA=7",
                    // Container options
                    "-movflags", "+faststart",
                    "-f", "mp4",
                    // Overwrite output file if exists
                    "-y",
                    outputFile.getAbsolutePath()
            );

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
                    Map.of("cpu", Quantity.parse("8"),
                            "memory", Quantity.parse("8Gi")),
                    Map.of("cpu", Quantity.parse("4"),
                            "memory", Quantity.parse("4Gi"))
            );

            Job job = new JobBuilder()
                    .withApiVersion("batch/v1")
                    .withNewMetadata()
                        .withName(podName)
                        .withLabels(Map.of(
                                "app", "ffmpeg",
                                JOB_ID_LABEL, transformPath(inputFile.getAbsolutePath()))
                        )
                    .endMetadata()
                    .withNewSpec()
                        .withBackoffLimit(1)
                        .withNewTemplate()
                            .withNewSpec()
                                .addNewContainer()
                                    .withName(podName)
                                    .withImage("linuxserver/ffmpeg:latest")
                                    .withArgs(args)
                                    .withVolumeMounts(volumeMounts)
                                    .withResources(resources)
                                .endContainer()
                                .withVolumes(volumes)
                                .withRestartPolicy("Never")
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                    .build();

            client.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        }
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

    private String transformPath(String path) {
        return path.split(MediaPath.MEDIA_ROOT_FOLDER)[1].replaceAll("[^A-Za-z0-9]", "-");
    }
}
