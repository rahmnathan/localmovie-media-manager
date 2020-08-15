package com.github.rahmnathan.localmovie.control;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class FFProbeService {
    private FFprobe fFprobe;

    @EventListener(ApplicationReadyEvent.class)
    public void initFFprobe() {
        try {
            this.fFprobe = new FFprobe("ffprobe");
        } catch (IOException e) {
            log.warn("Failure loading FFprobe. Won't be able to determine video duration.");
        }
    }

    public Double loadDuration(File file) {
        try {
            FFmpegProbeResult probeResult = fFprobe.probe(file.getAbsolutePath());
            return probeResult.getFormat().duration;
        } catch (IOException e){
            log.error("Failure to determine video duration.", e);
            return Double.NaN;
        }
    }
}
