package com.github.rahmnathan.localmovie.media.event;

import java.io.File;

public final class MediaJobArtifacts {
    private static final String SUBTITLE_SYNC_PREFIX = ".subtitle-sync-";

    private MediaJobArtifacts() {
    }

    public static boolean isSubtitleSyncArtifact(File file) {
        File current = file;
        while (current != null) {
            if (current.getName().startsWith(SUBTITLE_SYNC_PREFIX)) {
                return true;
            }
            current = current.getParentFile();
        }

        return false;
    }
}
