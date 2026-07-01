package com.github.rahmnathan.localmovie.media.event;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaEventMonitorTest {

    @Mock
    private MediaJobRepository mediaJobRepository;
    @Mock
    private MediaEventService eventService;
    @Mock
    private LockRegistry lockRegistry;
    @Mock
    private Lock lock;

    private MediaEventMonitor mediaEventMonitor;

    @BeforeEach
    void setUp() {
        ServiceConfig serviceConfig = new ServiceConfig();
        ServiceConfig.ConversionServiceConfig conversionServiceConfig = new ServiceConfig.ConversionServiceConfig();
        conversionServiceConfig.setEnabled(true);
        serviceConfig.setConversionService(conversionServiceConfig);

        when(lockRegistry.obtain(anyString())).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);

        mediaEventMonitor = new MediaEventMonitor(mediaJobRepository, eventService, serviceConfig, lockRegistry);
    }

    @Test
    void directoryModified_skipsCreateEventForCompletedConversionOutput(@TempDir Path tempDir) throws Exception {
        File outputFile = mediaFile(tempDir, "Converted.mp4").toFile();
        when(mediaJobRepository.existsByOutputFileAndStatusIn(eq(outputFile.toString()), anySet()))
                .thenAnswer(invocation -> {
                    Set<String> statuses = invocation.getArgument(1);
                    return statuses.contains(MediaJobStatus.SUCCEEDED.name());
                });

        mediaEventMonitor.directoryModified(StandardWatchEventKinds.ENTRY_CREATE, outputFile);

        verify(mediaJobRepository, never()).save(any());
        verify(eventService, never()).handleCreateEvent(any());
    }

    @Test
    void directoryModified_clearsCompletedOutputHistoryWhenOutputIsDeleted(@TempDir Path tempDir) throws Exception {
        File outputFile = mediaFile(tempDir, "Deleted.mp4").toFile();
        MediaJob completedJob = MediaJob.builder()
                .outputFile(outputFile.toString())
                .status(MediaJobStatus.SUCCEEDED.name())
                .build();
        when(mediaJobRepository.findAllByOutputFile(outputFile.toString())).thenReturn(List.of(completedJob));

        mediaEventMonitor.directoryModified(StandardWatchEventKinds.ENTRY_DELETE, outputFile);

        verify(mediaJobRepository).deleteAll(List.of(completedJob));
        verify(eventService).handleDeleteEvent(any());
    }

    @Test
    void directoryModified_queuesConversionAfterCompletedOutputHistoryWasCleared(@TempDir Path tempDir) throws Exception {
        File readdedFile = mediaFile(tempDir, "Readded.mp4").toFile();
        Files.writeString(readdedFile.toPath(), "stable media file");
        when(mediaJobRepository.existsByOutputFileAndStatusIn(eq(readdedFile.toString()), anySet())).thenReturn(false);
        when(mediaJobRepository.existsByInputFileAndStatusIn(eq(readdedFile.toString()), anySet())).thenReturn(false);

        mediaEventMonitor.directoryModified(StandardWatchEventKinds.ENTRY_CREATE, readdedFile);

        ArgumentCaptor<MediaJob> mediaJobCaptor = ArgumentCaptor.forClass(MediaJob.class);
        verify(mediaJobRepository).save(mediaJobCaptor.capture());
        assertEquals(readdedFile.toString(), mediaJobCaptor.getValue().getInputFile());
        assertEquals(readdedFile.toString().replace(".mp4", ".mkv"), mediaJobCaptor.getValue().getOutputFile());
    }

    private Path mediaFile(Path tempDir, String fileName) throws Exception {
        Path mediaDirectory = tempDir.resolve("LocalMedia").resolve("Movies");
        Files.createDirectories(mediaDirectory);
        return mediaDirectory.resolve(fileName);
    }
}
