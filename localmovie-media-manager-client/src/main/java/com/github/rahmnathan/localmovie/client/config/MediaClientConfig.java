package com.github.rahmnathan.localmovie.client.config;

public class MediaClientConfig {
    private final String mediaManagerUrl;

    public MediaClientConfig(String mediaManagerUrl) {
        this.mediaManagerUrl = mediaManagerUrl;
    }

    public String getMediaManagerUrl() {
        return mediaManagerUrl;
    }
}
