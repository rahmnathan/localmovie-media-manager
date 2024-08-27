package com.github.rahmnathan.localmovie.control.event.files;

import java.io.File;
import java.nio.file.WatchEvent;

public interface DirectoryMonitorObserver {
    void directoryModified(WatchEvent.Kind event, File absolutePath);
}
