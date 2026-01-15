package com.github.rahmnathan.localmovie.media.job;

import com.github.rahmnathan.localmovie.TestContainersConfiguration;
import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.media.event.MediaEventService;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import(TestContainersConfiguration.class)
class MediaJobServiceTest {

    @MockitoBean
    KubernetesService kubernetesService;

    @MockitoBean
    MediaEventService mediaEventService;

    private final MediaJobService mediaJobService;
    private final MediaJobRepository jobRepository;

    @Autowired
    MediaJobServiceTest(MediaJobService mediaJobService, MediaJobRepository jobRepository, MediaInitializer initializer) {
        this.mediaJobService = mediaJobService;
        this.jobRepository = jobRepository;

        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLaunchQueuedJob() throws Exception {
        MediaJob mediaJob = MediaJob.builder()
                .inputFile("input-path")
                .outputFile("output-path")
                .jobId("job-id")
                .handbrakePreset("preset")
                .status(MediaJobStatus.QUEUED.name())
                .build();

        jobRepository.save(mediaJob);

        mediaJobService.scanQueuedJobs();

        verify(kubernetesService).launchVideoConverter(any(), any());
    }

    @Test
    void testUpdateJobStatus() throws Exception {
        MediaJob mediaJob = MediaJob.builder()
                .inputFile("/tmp/LocalMedia/Movies/localmovies-test-input-file.txt")
                .outputFile("/tmp/LocalMedia/Movies/localmovies-test-input-file.txt")
                .jobId("job-id")
                .handbrakePreset("preset")
                .status(MediaJobStatus.RUNNING.name())
                .build();

        jobRepository.save(mediaJob);

        new File("/tmp/LocalMedia/Movies/").mkdir();

        when(kubernetesService.getJobStatus("job-id")).thenReturn(Optional.of(MediaJobStatus.SUCCEEDED));

        mediaJobService.updateJobStatus();

        verify(kubernetesService).deleteJob("job-id");
    }

    @Test
    void testRecordETAs() throws Exception {
        MediaJob mediaJob = MediaJob.builder()
                .inputFile("/tmp/LocalMedia/localmovies-test-input-file.txt")
                .outputFile("/tmp/LocalMedia/localmovies-test-input-file.txt")
                .jobId("job-id2")
                .handbrakePreset("preset")
                .status(MediaJobStatus.RUNNING.name())
                .build();

        jobRepository.save(mediaJob);

        when(kubernetesService.getETA("job-id2")).thenReturn(Optional.of(Duration.of(1, ChronoUnit.MINUTES)));

        mediaJobService.extractAndRecordETAs();

        verify(kubernetesService).getETA("job-id2");
    }
}
