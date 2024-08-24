package com.github.rahmnathan.localmovie.control.job;

import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@AllArgsConstructor
public class KubernetesService {

    private static final String HANDBRAKE_PRESET = "Chromecast 1080p60 Surround";

    private static final Pattern ETA_PATTERN = Pattern.compile("\\d\\dh\\d\\d");

    private final MeterRegistry meterRegistry;

    public void launchVideoConverter(File inputFile, File outputFile) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {

            Optional<Pod> localmoviesPodOptional = client.pods().list().getItems().stream()
                    .filter(pod -> "localmovies".equalsIgnoreCase(pod.getMetadata().getLabels().get("app")))
                    .findAny();

            if (localmoviesPodOptional.isEmpty()) {
                return;
            }

            String podName = "handbrake-" + UUID.randomUUID();

            log.info("Creating job {} to process media conversion.", podName);

            List<String> args = List.of("-Z", HANDBRAKE_PRESET,
                    "-i", inputFile.getAbsolutePath(),
                    "-o", outputFile.getAbsolutePath(),
                    "-v");

            List<Volume> volumes = localmoviesPodOptional.get().getSpec().getVolumes().stream()
                    .filter(volume -> volume.getName().startsWith("media"))
                    .toList();

            List<VolumeMount> volumeMounts = localmoviesPodOptional.get().getSpec().getContainers().stream()
                    .filter(container -> "localmovies".equalsIgnoreCase(container.getName()))
                    .findAny()
                    .get()
                    .getVolumeMounts().stream()
                    .filter(volumeMount -> volumeMount.getName().startsWith("media"))
                    .toList();

            String namespace = getNamespace();

            ResourceRequirements resources = new ResourceRequirements(
                    new ArrayList<>(),
                    Map.of("cpu", Quantity.parse("6"),
                            "memory", Quantity.parse("6Gi")),
                    Map.of("cpu", Quantity.parse("2"),
                            "memory", Quantity.parse("2Gi"))
            );

            Job job = new JobBuilder()
                    .withApiVersion("batch/v1")
                    .withNewMetadata()
                        .withName(podName)
                        .withLabels(Map.of(
                                "app", "handbrake",
                                "jobId", transformPath(inputFile.getAbsolutePath()))
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

            client.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        }
    }

    public MediaJobStatus getJobStatus(String jobId) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {

            Optional<JobStatus> jobStatusOptional = client.batch().v1().jobs().inNamespace(getNamespace())
                    .withLabel("jobId", jobId).list().getItems()
                    .stream()
                    .findFirst()
                    .map(Job::getStatus);

            if (jobStatusOptional.isEmpty()) {
                return MediaJobStatus.UNKNOWN;
            }

            JobStatus jobStatus = jobStatusOptional.get();

            if (jobStatus.getSucceeded() != null && jobStatus.getSucceeded() > 0) {
                return MediaJobStatus.SUCCEEDED;
            } else if (jobStatus.getFailed() != null && jobStatus.getFailed() > 0) {
                return MediaJobStatus.FAILED;
            } else if (jobStatus.getActive() != null && jobStatus.getActive() > 0) {
                return MediaJobStatus.RUNNING;
            }

            return MediaJobStatus.UNKNOWN;
        }
    }

    public void deleteJob(String jobId) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {
            client.batch().v1().jobs().inNamespace(getNamespace()).withName(jobId).delete();
        }
    }

    public void extractAndRecordETAs() throws Exception {
        String namespace = getNamespace();

        try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {

            List<Pod> pods = client.pods().inNamespace(namespace).list().getItems().stream()
                    .filter(pod -> pod.getMetadata().getName().startsWith("handbrake"))
                    .filter(pod -> pod.getStatus().getPhase().equalsIgnoreCase("running"))
                    .toList();

            for(Pod pod : pods) {
                String podName = pod.getMetadata().getName();
                String podLog = client.pods().inNamespace(namespace).withName(podName).tailingLines(1).getLog();

                // Extracting hours and minutes from log statement.
                // Example pod log: "ETA 01h25m15s)"
                Matcher etaMatcher = ETA_PATTERN.matcher(podLog);
                if(etaMatcher.find()) {
                    String eta = etaMatcher.group();
                    log.info("Found Handbrake ETA in logs ({}). Recording metric.", eta);

                    String[] time = eta.split("h");
                    int minutesRemaining = (Integer.parseInt(time[0]) * 60) + Integer.parseInt(time[1]);

                    String jobId = client.batch().v1().jobs().inNamespace(namespace).withName(pod.getMetadata().getLabels().get("job-name")).get()
                            .getMetadata().getLabels().get("jobId");

                    meterRegistry.timer("conversion.time.left", List.of(Tag.of("jobId", jobId))).record(Duration.ofMinutes(minutesRemaining));
                }
            }

        }
    }

    private String getNamespace() throws IOException {
        return Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace"));
    }

    private String transformPath(String path) {
        return path.split(File.separator + "LocalMedia" + File.separator)[1].replaceAll("[^A-Za-z0-9]", "-");
    }
}
