package com.github.rahmnathan.localmovie.web.webapp;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ClientCapabilities {
    private Set<String> supportedVideoCodecs;
    private Set<String> supportedAudioCodecs;
    private Set<String> supportedContainers;

    public boolean supports(String videoCodec, String audioCodec, String container) {
        boolean videoSupported = videoCodec == null ||
            supportedVideoCodecs.stream().anyMatch(c -> c.equalsIgnoreCase(videoCodec));
        boolean audioSupported = audioCodec == null ||
            supportedAudioCodecs.stream().anyMatch(c -> c.equalsIgnoreCase(audioCodec));
        boolean containerSupported = container == null ||
            supportedContainers.stream().anyMatch(c -> c.equalsIgnoreCase(container));

        return videoSupported && audioSupported && containerSupported;
    }
}
