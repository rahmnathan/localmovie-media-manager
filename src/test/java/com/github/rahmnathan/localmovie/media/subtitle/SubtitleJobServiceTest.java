package com.github.rahmnathan.localmovie.media.subtitle;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.SubtitleJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.SubtitleJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaSubtitleRepository;
import com.github.rahmnathan.localmovie.persistence.repository.SubtitleJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubtitleJobServiceTest {

    @Mock
    private SubtitleJobRepository subtitleJobRepository;
    @Mock
    private MediaSubtitleRepository subtitleRepository;
    @Mock
    private SubtitleProvider subtitleProvider;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private ServiceConfig serviceConfig;
    @Mock
    private ServiceConfig.OpenSubtitlesConfig openSubtitlesConfig;
    @Mock
    private Timer timer;

    @Captor
    private ArgumentCaptor<SubtitleJob> jobCaptor;

    private SubtitleJobService subtitleJobService;

    @BeforeEach
    void setUp() {
        when(serviceConfig.getOpensubtitles()).thenReturn(openSubtitlesConfig);
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        subtitleJobService = new SubtitleJobService(
                subtitleJobRepository,
                subtitleRepository,
                subtitleProvider,
                meterRegistry,
                serviceConfig
        );
    }

    @Test
    void processSubtitleJobs_whenDisabled_doesNothing() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(false);

        subtitleJobService.processSubtitleJobs();

        verifyNoInteractions(subtitleJobRepository);
        verifyNoInteractions(subtitleProvider);
    }

    @Test
    void processSubtitleJobs_whenQuotaExhausted_skipsProcessing() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(false);

        subtitleJobService.processSubtitleJobs();

        verify(subtitleJobRepository, never()).findAllByStatusOrderByCreatedAsc(any());
    }

    @Test
    void processSubtitleJobs_processesQueuedJobs() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED)).thenReturn(1);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getMediaFileId()).thenReturn("test-media-123");

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId("tt1234567")
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(0)
                .build();
        when(subtitleJobRepository.findAllByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED))
                .thenReturn(List.of(job));

        SubtitleResult subtitleResult = SubtitleResult.builder()
                .content("WEBVTT")
                .languageCode("en")
                .format("vtt")
                .opensubtitlesId("sub123")
                .build();
        when(subtitleProvider.fetchSubtitle("tt1234567")).thenReturn(Optional.of(subtitleResult));

        subtitleJobService.processSubtitleJobs();

        verify(subtitleRepository).save(any());
        verify(subtitleJobRepository, atLeast(2)).save(jobCaptor.capture());

        // Verify the job ends up in SUCCEEDED status
        List<SubtitleJob> savedJobs = jobCaptor.getAllValues();
        SubtitleJob finalJob = savedJobs.get(savedJobs.size() - 1);
        assertEquals(SubtitleJobStatus.SUCCEEDED, finalJob.getStatus());
    }

    @Test
    void processSubtitleJobs_handlesNotFoundSubtitle() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED)).thenReturn(1);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getMediaFileId()).thenReturn("test-media-123");

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId("tt1234567")
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(0)
                .build();
        when(subtitleJobRepository.findAllByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED))
                .thenReturn(List.of(job));

        when(subtitleProvider.fetchSubtitle("tt1234567")).thenReturn(Optional.empty());

        subtitleJobService.processSubtitleJobs();

        verify(subtitleJobRepository, atLeast(2)).save(jobCaptor.capture());
        List<SubtitleJob> savedJobs = jobCaptor.getAllValues();
        assertEquals(SubtitleJobStatus.NOT_FOUND, savedJobs.get(savedJobs.size() - 1).getStatus());
    }

    @Test
    void processSubtitleJobs_handlesQuotaExceeded() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED)).thenReturn(1);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getMediaFileId()).thenReturn("test-media-123");

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId("tt1234567")
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(0)
                .build();
        when(subtitleJobRepository.findAllByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED))
                .thenReturn(List.of(job));

        when(subtitleProvider.fetchSubtitle("tt1234567"))
                .thenThrow(new DownloadQuotaExceededException());

        subtitleJobService.processSubtitleJobs();

        verify(subtitleJobRepository, atLeast(2)).save(jobCaptor.capture());
        List<SubtitleJob> savedJobs = jobCaptor.getAllValues();
        assertEquals(SubtitleJobStatus.QUOTA_EXCEEDED, savedJobs.get(savedJobs.size() - 1).getStatus());
    }

    @Test
    void processSubtitleJobs_handlesNoImdbId() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED)).thenReturn(1);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getMediaFileId()).thenReturn("test-media-123");

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId(null)
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(0)
                .build();
        when(subtitleJobRepository.findAllByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED))
                .thenReturn(List.of(job));

        subtitleJobService.processSubtitleJobs();

        verify(subtitleProvider, never()).fetchSubtitle(any());
        verify(subtitleJobRepository, atLeast(2)).save(jobCaptor.capture());
        List<SubtitleJob> savedJobs = jobCaptor.getAllValues();
        assertEquals(SubtitleJobStatus.NOT_FOUND, savedJobs.get(savedJobs.size() - 1).getStatus());
    }

    @Test
    void processSubtitleJobs_retriesOnException() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED)).thenReturn(1);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getMediaFileId()).thenReturn("test-media-123");

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId("tt1234567")
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(0)
                .build();
        when(subtitleJobRepository.findAllByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED))
                .thenReturn(List.of(job));

        when(subtitleProvider.fetchSubtitle("tt1234567"))
                .thenThrow(new RuntimeException("Network error"));

        subtitleJobService.processSubtitleJobs();

        verify(subtitleJobRepository, atLeast(2)).save(jobCaptor.capture());
        List<SubtitleJob> savedJobs = jobCaptor.getAllValues();
        SubtitleJob lastSaved = savedJobs.get(savedJobs.size() - 1);
        assertEquals(SubtitleJobStatus.QUEUED, lastSaved.getStatus());
        assertEquals(1, lastSaved.getRetryCount());
    }

    @Test
    void processSubtitleJobs_failsAfterMaxRetries() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED)).thenReturn(1);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getMediaFileId()).thenReturn("test-media-123");

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId("tt1234567")
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(2) // Already at max - 1
                .build();
        when(subtitleJobRepository.findAllByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED))
                .thenReturn(List.of(job));

        when(subtitleProvider.fetchSubtitle("tt1234567"))
                .thenThrow(new RuntimeException("Network error"));

        subtitleJobService.processSubtitleJobs();

        verify(subtitleJobRepository, atLeast(2)).save(jobCaptor.capture());
        List<SubtitleJob> savedJobs = jobCaptor.getAllValues();
        SubtitleJob lastSaved = savedJobs.get(savedJobs.size() - 1);
        assertEquals(SubtitleJobStatus.FAILED, lastSaved.getStatus());
        assertEquals(3, lastSaved.getRetryCount());
    }

    @Test
    void queueSubtitleFetch_whenDisabled_doesNothing() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(false);

        MediaFile mediaFile = mock(MediaFile.class);
        subtitleJobService.queueSubtitleFetch(mediaFile, "tt1234567");

        verifyNoInteractions(subtitleJobRepository);
    }

    @Test
    void queueSubtitleFetch_skipsIfJobExists() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getId()).thenReturn(1L);
        when(subtitleJobRepository.existsByMediaFileIdAndStatusIn(eq(1L), any()))
                .thenReturn(true);

        subtitleJobService.queueSubtitleFetch(mediaFile, "tt1234567");

        verify(subtitleJobRepository, never()).save(any());
    }

    @Test
    void queueSubtitleFetch_skipsIfSubtitleExists() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getId()).thenReturn(1L);
        when(subtitleJobRepository.existsByMediaFileIdAndStatusIn(eq(1L), any()))
                .thenReturn(false);
        when(subtitleRepository.existsByMediaFileIdAndLanguageCode(1L, "en"))
                .thenReturn(true);

        subtitleJobService.queueSubtitleFetch(mediaFile, "tt1234567");

        verify(subtitleJobRepository, never()).save(any());
    }

    @Test
    void queueSubtitleFetch_createsJob() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);

        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.getId()).thenReturn(1L);
        when(mediaFile.getMediaFileId()).thenReturn("media-123");
        when(subtitleJobRepository.existsByMediaFileIdAndStatusIn(eq(1L), any()))
                .thenReturn(false);
        when(subtitleRepository.existsByMediaFileIdAndLanguageCode(1L, "en"))
                .thenReturn(false);

        subtitleJobService.queueSubtitleFetch(mediaFile, "tt1234567");

        verify(subtitleJobRepository).save(jobCaptor.capture());
        SubtitleJob saved = jobCaptor.getValue();
        assertEquals(mediaFile, saved.getMediaFile());
        assertEquals("tt1234567", saved.getImdbId());
        assertEquals(SubtitleJobStatus.QUEUED, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
    }

    @Test
    void checkQuotaAndRequeueJobs_whenDisabled_doesNothing() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(false);

        subtitleJobService.checkQuotaAndRequeueJobs();

        verify(subtitleJobRepository, never()).countAllByStatus(any());
    }

    @Test
    void checkQuotaAndRequeueJobs_whenNoBlockedJobs_doesNothing() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED)).thenReturn(0);

        subtitleJobService.checkQuotaAndRequeueJobs();

        verify(subtitleProvider, never()).refreshQuota();
    }

    @Test
    void checkQuotaAndRequeueJobs_requeuesBlockedJobs() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED)).thenReturn(2);

        MediaFile mediaFile1 = mock(MediaFile.class);
        MediaFile mediaFile2 = mock(MediaFile.class);

        SubtitleJob job1 = SubtitleJob.builder()
                .mediaFile(mediaFile1)
                .imdbId("tt111")
                .status(SubtitleJobStatus.QUOTA_EXCEEDED)
                .errorMessage("Download quota exceeded")
                .build();
        SubtitleJob job2 = SubtitleJob.builder()
                .mediaFile(mediaFile2)
                .imdbId("tt222")
                .status(SubtitleJobStatus.QUOTA_EXCEEDED)
                .errorMessage("Download quota exceeded")
                .build();

        when(subtitleJobRepository.findAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED))
                .thenReturn(List.of(job1, job2));
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(true);
        when(subtitleProvider.getRemainingDownloads()).thenReturn(100);

        subtitleJobService.checkQuotaAndRequeueJobs();

        verify(subtitleProvider).refreshQuota();
        verify(subtitleJobRepository).saveAll(anyList());

        assertEquals(SubtitleJobStatus.QUEUED, job1.getStatus());
        assertNull(job1.getErrorMessage());
        assertEquals(SubtitleJobStatus.QUEUED, job2.getStatus());
        assertNull(job2.getErrorMessage());
    }

    @Test
    void checkQuotaAndRequeueJobs_doesNotRequeueWhenStillExhausted() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED)).thenReturn(2);
        when(subtitleProvider.hasDownloadsRemaining()).thenReturn(false);

        subtitleJobService.checkQuotaAndRequeueJobs();

        verify(subtitleProvider).refreshQuota();
        verify(subtitleJobRepository, never()).findAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED);
        verify(subtitleJobRepository, never()).saveAll(anyList());
    }
}
