package com.github.rahmnathan.localmovie.media.job;

import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class KubernetesService {

    private static final String JOB_NAME = "job-name";
    private static final String JOB_ID_LABEL = "jobId";

    private static final Pattern ETA_PATTERN = Pattern.compile("\\d\\dh\\d\\d");

    public Optional<MediaJobStatus> getJobStatus(String jobId) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {

            Optional<JobStatus> jobStatusOptional = client.batch().v1().jobs().inNamespace(getNamespace())
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
    }

    public void deleteJob(String jobId) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            Optional<Job> jobOptional = client.batch().v1().jobs()
                    .inNamespace(getNamespace())
                    .withLabel(JOB_ID_LABEL, jobId).list().getItems().stream()
                    .findAny();

            if(jobOptional.isPresent()) {
                client.batch().v1().jobs().inNamespace(getNamespace()).resource(jobOptional.get()).delete();
            } else {
                log.warn("No kubernetes job found for jobId: {}", jobId);
            }
        }
    }

    public Optional<Duration> getETA(String jobId) throws IOException {
        String namespace = getNamespace();

        try (KubernetesClient client = new KubernetesClientBuilder().build()) {

            // Lookup job by jobId
            List<Job> jobList = client.batch().v1().jobs()
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
            Optional<Pod> podOptional = client.pods()
                    .inNamespace(namespace)
                    .withLabel(JOB_NAME, jobName)
                    .list().getItems().stream()
                    .filter(pod -> pod.getStatus().getPhase().equalsIgnoreCase("running"))
                    .findAny();

            if (podOptional.isEmpty()) {
                log.warn("No running pod for jobId: {}", jobId);
                return Optional.empty();
            }

            String podName = podOptional.get().getMetadata().getName();

            String podLog = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .tailingLines(1)
                    .getLog();

            // Extracting hours and minutes from log statement.
            // Example pod log: "ETA 01h25m15s)"
            Matcher etaMatcher = ETA_PATTERN.matcher(podLog);
            if (etaMatcher.find()) {
                String eta = etaMatcher.group();
                log.debug("Found Handbrake ETA in logs ({}). Recording metric.", eta);

                String[] time = eta.split("h");
                int minutesRemaining = (Integer.parseInt(time[0]) * 60) + Integer.parseInt(time[1]);

                return Optional.of(Duration.ofMinutes(minutesRemaining));
            }
        }

        return Optional.empty();
    }

    private String getNamespace() throws IOException {
        Path namespaceFile = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (namespaceFile.toFile().exists()) {
            return Files.readString(namespaceFile);
        }

        return "localmovies";
    }
}
