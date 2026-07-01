package com.github.rahmnathan.localmovie.media.job;

import com.github.rahmnathan.localmovie.data.MediaPath;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Component("handbrakeVideoConverter")
@RequiredArgsConstructor
public class HandbrakeVideoConverter implements VideoConverter {

    private static final String JOB_ID_LABEL = "jobId";
    private static final String HANDBRAKE_PRESET = "Chromecast 1080p60 Surround";

    private final KubernetesClient kubernetesClient;

    @Override
    public void launchVideoConverter(File inputFile, File outputFile) throws IOException {
        String namespace = getNamespace();

        Optional<Pod> localmoviesPodOptional = kubernetesClient.pods().inNamespace(namespace).list().getItems().stream()
                .filter(pod -> "localmovies".equalsIgnoreCase(labelValue(pod, "app")))
                .findAny();

        if (localmoviesPodOptional.isEmpty()) {
            throw new IOException("Could not find localmovies pod to copy volume mounts from");
        }

        String podName = "handbrake-" + UUID.randomUUID();

        log.info("Creating Handbrake job with name: {}", podName);

        // https://handbrake.fr/docs/en/latest/cli/command-line-reference.html
        List<String> args = List.of("-Z", HANDBRAKE_PRESET,
                "-i", inputFile.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath(),
                "--audio-lang-list", "eng,und",
                "--first-audio",
                "--aencoder", "av_aac",
                "--mixdown", "stereo",
                "--drc", "1.2",
                "--gain", "1",
                "-v"
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
                            "app", "handbrake",
                            JOB_ID_LABEL, transformPath(inputFile.getAbsolutePath()))
                    )
                .endMetadata()
                .withNewSpec()
                    .withBackoffLimit(1)
                    .withNewTemplate()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(podName)
                                .withImage("rahmnathan/handbrake:latest")
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

    private String transformPath(String path) {
        return path.split(MediaPath.MEDIA_ROOT_FOLDER)[1].replaceAll("[^A-Za-z0-9]", "-");
    }
}
