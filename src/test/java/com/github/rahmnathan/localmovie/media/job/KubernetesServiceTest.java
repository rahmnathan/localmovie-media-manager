package com.github.rahmnathan.localmovie.media.job;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KubernetesServiceTest {

    private final KubernetesService kubernetesService = new KubernetesService();

    @Test
    void parsesHandbrakeEta() {
        Optional<Duration> eta = kubernetesService.parseETA("Encoding: task 1 of 1, 42.00 % (ETA 01h25m15s)", null);

        assertThat(eta).contains(Duration.ofHours(1).plusMinutes(25).plusSeconds(15));
    }

    @Test
    void parsesFfmpegProgressEta() {
        String podLog = """
                frame=100
                out_time=00:10:00.000000
                speed=2.00x
                progress=continue
                """;

        Optional<Duration> eta = kubernetesService.parseETA(podLog, 1800L);

        assertThat(eta).contains(Duration.ofMinutes(10));
    }

    @Test
    void parsesFfmpegStatsEta() {
        String podLog = "frame=100 fps=50 q=28.0 size=1024kB time=00:05:00.00 bitrate=1024.0kbits/s speed=1.5x";

        Optional<Duration> eta = kubernetesService.parseETA(podLog, 1200L);

        assertThat(eta).contains(Duration.ofMinutes(10));
    }

    @Test
    void parsesFfmpegEtaFromLogDuration() {
        String podLog = """
                Input #0, matroska,webm, from 'input.mkv':
                  Duration: 00:30:00.00, start: 0.000000, bitrate: 1000 kb/s
                out_time=00:10:00.000000
                speed=2.00x
                """;

        Optional<Duration> eta = kubernetesService.parseETA(podLog, null);

        assertThat(eta).contains(Duration.ofMinutes(10));
    }
}
