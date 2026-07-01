package com.github.rahmnathan.localmovie.media.event.files;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.StandardWatchEventKinds;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DirectoryMonitorListenerTest {

    @Test
    void onDirectoryCreate_ignoresSubtitleSyncArtifacts() {
        FileAlterationMonitor monitor = mock(FileAlterationMonitor.class);
        DirectoryMonitorObserver observer = mock(DirectoryMonitorObserver.class);
        DirectoryMonitorListener listener = new DirectoryMonitorListener(monitor, Set.of(observer));

        listener.onDirectoryCreate(new File("/media/Movie/.subtitle-sync-abc123-456"));

        verifyNoInteractions(observer);
        verify(monitor, never()).addObserver(any(FileAlterationObserver.class));
    }

    @Test
    void onFileCreate_ignoresFilesInsideSubtitleSyncArtifacts() {
        FileAlterationMonitor monitor = mock(FileAlterationMonitor.class);
        DirectoryMonitorObserver observer = mock(DirectoryMonitorObserver.class);
        DirectoryMonitorListener listener = new DirectoryMonitorListener(monitor, Set.of(observer));

        listener.onFileCreate(new File("/media/Movie/.subtitle-sync-abc123-456/input.vtt"));

        verifyNoInteractions(observer);
        verifyNoInteractions(monitor);
    }

    @Test
    void onDirectoryCreate_notifiesObserversForNormalDirectory() throws Exception {
        FileAlterationMonitor monitor = mock(FileAlterationMonitor.class);
        DirectoryMonitorObserver observer = mock(DirectoryMonitorObserver.class);
        DirectoryMonitorListener listener = new DirectoryMonitorListener(monitor, Set.of(observer));
        File directory = new File("/media/Movie/Extras");

        listener.onDirectoryCreate(directory);

        verify(observer, timeout(1000))
                .directoryModified(StandardWatchEventKinds.ENTRY_CREATE, directory);
        verify(monitor).addObserver(any(FileAlterationObserver.class));
    }
}
