package com.github.rahmnathan.localmovie.media.recommendation;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.rahmnathan.localmovie.web.filter.LoggingFilter.X_CORRELATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationJobService {

    private final RecommendationService recommendationService;
    private final MediaUserRepository userRepository;
    private final MeterRegistry meterRegistry;
    private final ServiceConfig serviceConfig;

    /**
     * Refresh recommendations for all users every 6 hours.
     * Since Ollama can be slow, this runs in the background and stores results
     * for quick retrieval via the API.
     */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L) // 6 hours
    @SchedulerLock(name = "refresh-recommendations-lock", lockAtMostFor = "PT30M")
    public void refreshRecommendations() {
        if (serviceConfig.getOllama() == null || !serviceConfig.getOllama().isEnabled()) {
            log.warn("Ollama is not available/enabled.");
            return;
        }

        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        try {
            log.info("Starting recommendation refresh job");

            // Get all users who have watch history
            var users = userRepository.findAll();
            int processedCount = 0;

            for (var user : users) {
                try {
                    long userStart = System.currentTimeMillis();
                    RecommendationService.GenerationReport report = recommendationService.generateRecommendationsForUser(user.getUserId());
                    meterRegistry.counter("localmovies.recommendations.generate",
                            "source", report.source().name().toLowerCase(),
                            "persisted", String.valueOf(report.persisted()),
                            "usedExisting", String.valueOf(report.usedExisting()),
                            "lowQuality", String.valueOf(report.lowQuality()))
                            .increment();
                    meterRegistry.timer("localmovies.recommendations.user")
                            .record(System.currentTimeMillis() - userStart, TimeUnit.MILLISECONDS);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Failed to generate recommendations for user: {}", user.getUserId(), e);
                    meterRegistry.counter("localmovies.recommendations.generate.errors").increment();
                }
            }

            log.info("Recommendation refresh completed. Processed {} users", processedCount);
        } finally {
            MDC.clear();
            meterRegistry.timer("localmovies.recommendations.refresh")
                    .record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
        }
    }
}
