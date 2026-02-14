package com.github.rahmnathan.localmovie.media.subtitle;

public class DownloadQuotaExceededException extends Exception {
    public DownloadQuotaExceededException() {
        super("OpenSubtitles download quota exceeded");
    }
}
