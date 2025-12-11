package com.github.rahmnathan.localmovie.web.webapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ClientCapabilityService {

    public ClientCapabilities detectCapabilities(HttpHeaders headers) {
        String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);

        Set<String> videoCodecs = new HashSet<>();
        Set<String> audioCodecs = new HashSet<>();
        Set<String> containers = new HashSet<>();

        // Detect capabilities based on User-Agent
        if (userAgent != null) {
            String lowerUA = userAgent.toLowerCase();

            // Chrome capabilities
            if (lowerUA.contains("chrome") && !lowerUA.contains("edg")) {
                videoCodecs.addAll(Set.of("h264", "avc", "vp8", "vp9", "av1"));
                audioCodecs.addAll(Set.of("aac", "mp3", "opus", "vorbis"));
                containers.addAll(Set.of("mp4", "webm"));
                log.debug("Detected Chrome client");
            }
            // Firefox capabilities
            else if (lowerUA.contains("firefox")) {
                videoCodecs.addAll(Set.of("h264", "avc", "vp8", "vp9", "av1"));
                audioCodecs.addAll(Set.of("aac", "mp3", "opus", "vorbis"));
                containers.addAll(Set.of("mp4", "webm"));
                log.debug("Detected Firefox client");
            }
            // Safari capabilities
            else if (lowerUA.contains("safari") && !lowerUA.contains("chrome")) {
                videoCodecs.addAll(Set.of("h264", "avc", "hevc", "h265"));
                audioCodecs.addAll(Set.of("aac", "mp3"));
                containers.addAll(Set.of("mp4", "mov"));
                log.debug("Detected Safari client");
            }
            // Edge capabilities
            else if (lowerUA.contains("edg")) {
                videoCodecs.addAll(Set.of("h264", "avc", "vp9", "av1"));
                audioCodecs.addAll(Set.of("aac", "mp3", "opus"));
                containers.addAll(Set.of("mp4", "webm"));
                log.debug("Detected Edge client");
            }
            // iOS devices
            else if (lowerUA.contains("iphone") || lowerUA.contains("ipad")) {
                videoCodecs.addAll(Set.of("h264", "avc", "hevc", "h265"));
                audioCodecs.addAll(Set.of("aac", "mp3"));
                containers.addAll(Set.of("mp4", "mov"));
                log.debug("Detected iOS client");
            }
            // Android devices
            else if (lowerUA.contains("android")) {
                videoCodecs.addAll(Set.of("h264", "avc", "vp8", "vp9"));
                audioCodecs.addAll(Set.of("aac", "mp3", "opus"));
                containers.addAll(Set.of("mp4", "webm"));
                log.debug("Detected Android client");
            }
            // Fallback: assume basic H.264/AAC support
            else {
                videoCodecs.addAll(Set.of("h264", "avc"));
                audioCodecs.addAll(Set.of("aac", "mp3"));
                containers.addAll(Set.of("mp4"));
                log.debug("Unknown client, assuming basic H.264/AAC support");
            }
        } else {
            // No User-Agent: assume basic support
            videoCodecs.addAll(Set.of("h264", "avc"));
            audioCodecs.addAll(Set.of("aac", "mp3"));
            containers.addAll(Set.of("mp4"));
            log.debug("No User-Agent, assuming basic H.264/AAC support");
        }

        return ClientCapabilities.builder()
            .supportedVideoCodecs(videoCodecs)
            .supportedAudioCodecs(audioCodecs)
            .supportedContainers(containers)
            .build();
    }

    public String selectBestVideoCodec(ClientCapabilities capabilities, String currentCodec) {
        // Priority order: keep current if supported, then VP9, AV1, HEVC, H.264
        // VP9 and AV1 offer better compression than H.264

        // If client supports the current codec, no transcoding needed
        if (currentCodec != null &&
            capabilities.getSupportedVideoCodecs().stream().anyMatch(c -> c.equalsIgnoreCase(currentCodec))) {
            return null; // No transcoding needed
        }

        // Otherwise, pick best codec client supports
        if (capabilities.getSupportedVideoCodecs().contains("av1")) {
            return "av1"; // Best compression
        }
        if (capabilities.getSupportedVideoCodecs().contains("vp9")) {
            return "vp9"; // Good compression
        }
        if (capabilities.getSupportedVideoCodecs().contains("hevc") ||
            capabilities.getSupportedVideoCodecs().contains("h265")) {
            return "hevc"; // Good compression
        }
        // Fallback to H.264 (universal support)
        return "h264";
    }

    public String selectBestAudioCodec(ClientCapabilities capabilities, String currentCodec) {
        // If client supports the current codec, no transcoding needed
        if (currentCodec != null &&
            capabilities.getSupportedAudioCodecs().stream().anyMatch(c -> c.equalsIgnoreCase(currentCodec))) {
            return null; // No transcoding needed
        }

        // Otherwise, pick best codec client supports
        if (capabilities.getSupportedAudioCodecs().contains("opus")) {
            return "opus"; // Best quality
        }
        // Fallback to AAC (universal support)
        return "aac";
    }
}
