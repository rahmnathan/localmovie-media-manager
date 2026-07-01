package com.github.rahmnathan.localmovie.media.subtitle;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.SubtitleJobStatus;
import com.github.rahmnathan.localmovie.data.SubtitleSyncStatus;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaSubtitle;
import com.github.rahmnathan.localmovie.persistence.entity.SubtitleJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaSubtitleRepository;
import com.github.rahmnathan.localmovie.persistence.repository.SubtitleJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.rahmnathan.localmovie.web.filter.LoggingFilter.X_CORRELATION_ID;

@Slf4j
@Service
@AllArgsConstructor
public class SubtitleJobService {
    private static final int MAX_RETRY_COUNT = 3;

    private final SubtitleJobRepository subtitleJobRepository;
    private final MediaSubtitleRepository subtitleRepository;
    private final SubtitleProvider subtitleProvider;
    private final SubtitleSyncService subtitleSyncService;
    private final MeterRegistry meterRegistry;
    private final ServiceConfig serviceConfig;

    @Scheduled(fixedDelay = 60000L)
    @SchedulerLock(name = "process-subtitle-jobs-lock", lockAtMostFor = "PT30M")
    public void processSubtitleJobs() {
        if (!serviceConfig.getOpensubtitles().isEnabled()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        try {
            requeueStaleRunningJobs();

            // Check if we have downloads remaining before processing
            if (!subtitleProvider.hasDownloadsRemaining()) {
                log.info("OpenSubtitles download quota exhausted, skipping job processing");
                return;
            }

            int queuedCount = subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUEUED);
            if (queuedCount > 0) {
                log.info("Found {} queued subtitle jobs", queuedCount);
            }

            List<SubtitleJob> jobs = subtitleJobRepository
                    .findTop5ByStatusOrderByCreatedAsc(SubtitleJobStatus.QUEUED);

            for (SubtitleJob job : jobs) {
                if (!subtitleProvider.hasDownloadsRemaining()) {
                    log.info("OpenSubtitles download quota exhausted, stopping job processing");
                    break;
                }
                processJob(job);
            }
        } finally {
            MDC.clear();
            meterRegistry.timer("localmovies.subtitle-jobs.process")
                    .record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Scheduled task to check sync job status and complete jobs that have finished syncing.
     */
    @Scheduled(fixedDelay = 30000L)
    @SchedulerLock(name = "process-subtitle-sync-jobs-lock", lockAtMostFor = "PT10M")
    public void processSyncJobs() {
        if (!serviceConfig.getOpensubtitles().isEnabled()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString());

        try {
            List<SubtitleJob> syncingJobs = subtitleJobRepository.findAllBySyncStatus(SubtitleSyncStatus.RUNNING);

            if (!syncingJobs.isEmpty()) {
                log.info("Found {} jobs with running sync", syncingJobs.size());
            }

            for (SubtitleJob job : syncingJobs) {
                processSyncJob(job);
            }
        } finally {
            MDC.clear();
            meterRegistry.timer("localmovies.subtitle-sync-jobs.process")
                    .record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
        }
    }

    private void processSyncJob(SubtitleJob job) {
        String jobName = job.getSyncJobName();
        String tempDir = job.getSyncTempDir();

        try {
            SubtitleSyncStatus syncStatus = subtitleSyncService.checkSyncJobStatus(jobName);

            if (syncStatus == SubtitleSyncStatus.RUNNING) {
                // Check if sync has timed out
                LocalDateTime syncTimeout = LocalDateTime.now()
                        .minusSeconds(serviceConfig.getOpensubtitles().getSyncTimeoutSeconds());
                if (job.getUpdated() != null && job.getUpdated().isBefore(syncTimeout)) {
                    log.warn("Sync job {} timed out for MediaFile: {}, using unsynced subtitle",
                            jobName, job.getMediaFile().getMediaFileId());
                    syncStatus = SubtitleSyncStatus.FAILED;
                    job.setErrorMessage("Sync timed out after " + serviceConfig.getOpensubtitles().getSyncTimeoutSeconds() + " seconds");
                } else {
                    // Still running within timeout, check again next cycle
                    return;
                }
            }

            MediaFile mediaFile = job.getMediaFile();
            String subtitleContent;

            if (syncStatus == SubtitleSyncStatus.SUCCEEDED) {
                Optional<String> syncedContent = subtitleSyncService.collectSyncResult(jobName, tempDir);
                if (syncedContent.isPresent()) {
                    subtitleContent = syncedContent.get();
                    job.setSyncStatus(SubtitleSyncStatus.SUCCEEDED);
                    log.info("Subtitle sync succeeded for MediaFile: {}", mediaFile.getMediaFileId());
                } else {
                    // Sync job succeeded but couldn't read output, use original
                    subtitleContent = job.getUnsyncedContent();
                    job.setSyncStatus(SubtitleSyncStatus.FAILED);
                    job.setErrorMessage("Sync succeeded but could not read output file");
                    log.warn("Sync succeeded but could not read output for MediaFile: {}, using unsynced subtitle",
                            mediaFile.getMediaFileId());
                }
            } else {
                // Sync failed or timed out, use original unsynced content
                subtitleContent = job.getUnsyncedContent();
                job.setSyncStatus(SubtitleSyncStatus.FAILED);
                if (job.getErrorMessage() == null) {
                    String podLog = subtitleSyncService.getSyncJobLog(jobName);
                    job.setErrorMessage(truncate("Sync failed: " + podLog, 500));
                }
                log.warn("Subtitle sync failed for MediaFile: {}, using unsynced subtitle. Error: {}",
                        mediaFile.getMediaFileId(), job.getErrorMessage());
            }

            // Persist the subtitle (synced or unsynced)
            SubtitleResult subtitle = SubtitleResult.builder()
                    .content(subtitleContent)
                    .languageCode("en")
                    .format("vtt")
                    .opensubtitlesId(job.getOpensubtitlesId())
                    .build();
            upsertSubtitle(mediaFile, subtitle);

            // Mark job as succeeded (we got a subtitle either way)
            job.setStatus(SubtitleJobStatus.SUCCEEDED);
            job.setUnsyncedContent(null); // Clear to save space
            job.setOpensubtitlesId(null); // Clear since it's now in MediaSubtitle
            subtitleJobRepository.save(job);

            // Cleanup Kubernetes resources
            subtitleSyncService.cleanupSyncJob(jobName, tempDir);

            log.info("Completed subtitle job for MediaFile: {}", mediaFile.getMediaFileId());

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for sync job {}, skipping (will be retried)", jobName);
        } catch (Exception e) {
            log.error("Failed to process sync job {} for MediaFile: {}",
                    jobName, job.getMediaFile().getMediaFileId(), e);
        }
    }

    private void requeueStaleRunningJobs() {
        LocalDateTime staleBefore = LocalDateTime.now()
                .minusMinutes(serviceConfig.getOpensubtitles().getStaleRunningJobTimeoutMinutes());
        List<SubtitleJob> staleJobs = subtitleJobRepository
                .findAllByStatusAndUpdatedBefore(SubtitleJobStatus.RUNNING, staleBefore);

        if (staleJobs.isEmpty()) {
            return;
        }

        log.warn("Re-queuing {} stale subtitle jobs that were left RUNNING before {}", staleJobs.size(), staleBefore);
        staleJobs.forEach(job -> {
            job.setStatus(SubtitleJobStatus.QUEUED);
            job.setErrorMessage("Re-queued after stale RUNNING state");
            // Clean up any orphaned sync jobs
            if (job.getSyncJobName() != null) {
                subtitleSyncService.cleanupSyncJob(job.getSyncJobName(), job.getSyncTempDir());
                job.setSyncStatus(null);
                job.setSyncJobName(null);
                job.setSyncTempDir(null);
                job.setUnsyncedContent(null);
            }
        });
        subtitleJobRepository.saveAll(staleJobs);
    }

    private void processJob(SubtitleJob job) {
        job.setStatus(SubtitleJobStatus.RUNNING);
        subtitleJobRepository.save(job);

        try {
            MediaFile mediaFile = job.getMediaFile();
            String imdbId = job.getImdbId();
            Long mediaFileId = mediaFile.getId();

            String defaultLanguage = "en";
            if (mediaFileId != null && subtitleRepository.existsByMediaFileIdAndLanguageCode(mediaFileId, defaultLanguage)) {
                log.info("Subtitle already exists for MediaFile: {}, marking job as succeeded", mediaFile.getMediaFileId());
                job.setStatus(SubtitleJobStatus.SUCCEEDED);
                job.setErrorMessage(null);
                subtitleJobRepository.save(job);
                return;
            }

            if (imdbId == null || imdbId.isBlank()) {
                log.info("No IMDB ID available for MediaFile: {}", mediaFile.getMediaFileId());
                job.setStatus(SubtitleJobStatus.NOT_FOUND);
                job.setErrorMessage("No IMDB ID available");
                subtitleJobRepository.save(job);
                return;
            }

            Optional<SubtitleResult> result;
            try {
                result = subtitleProvider.fetchSubtitle(imdbId);
            } catch (DownloadQuotaExceededException e) {
                log.warn("Download quota exceeded while processing job for MediaFile: {}",
                        mediaFile.getMediaFileId());
                job.setStatus(SubtitleJobStatus.QUOTA_EXCEEDED);
                job.setErrorMessage("Download quota exceeded");
                subtitleJobRepository.save(job);
                return;
            }

            if (result.isEmpty()) {
                job.setStatus(SubtitleJobStatus.NOT_FOUND);
                job.setErrorMessage("Subtitle not found on OpenSubtitles");
                subtitleJobRepository.save(job);
                return;
            }

            SubtitleResult subtitle = result.get();
            log.info("Successfully fetched subtitle for MediaFile: {}", mediaFile.getMediaFileId());

            // Check if sync is enabled and launch async sync
            if (subtitleSyncService.isSyncEnabled()) {
                Optional<SubtitleSyncService.SyncJobInfo> syncInfo =
                        subtitleSyncService.launchSyncJob(mediaFile.getAbsolutePath(), subtitle.getContent());

                if (syncInfo.isPresent()) {
                    // Sync job launched, save state and wait for completion
                    job.setSyncStatus(SubtitleSyncStatus.RUNNING);
                    job.setSyncJobName(syncInfo.get().jobName());
                    job.setSyncTempDir(syncInfo.get().tempDir());
                    job.setUnsyncedContent(subtitle.getContent());
                    job.setOpensubtitlesId(subtitle.getOpensubtitlesId());
                    // Keep status as RUNNING - processSyncJobs will complete it
                    subtitleJobRepository.save(job);
                    log.info("Launched subtitle sync job {} for MediaFile: {}",
                            syncInfo.get().jobName(), mediaFile.getMediaFileId());
                    return;
                } else {
                    // Sync launch failed, continue without sync
                    log.warn("Failed to launch sync job for MediaFile: {}, saving unsynced subtitle",
                            mediaFile.getMediaFileId());
                    job.setSyncStatus(SubtitleSyncStatus.FAILED);
                }
            } else {
                job.setSyncStatus(SubtitleSyncStatus.SKIPPED);
            }

            // No sync or sync launch failed - persist subtitle directly
            upsertSubtitle(mediaFile, subtitle);
            job.setStatus(SubtitleJobStatus.SUCCEEDED);
            job.setErrorMessage(null);
            subtitleJobRepository.save(job);

        } catch (ObjectOptimisticLockingFailureException e) {
            // Job was modified by another process (e.g., requeueStaleRunningJobs), skip it
            log.warn("Optimistic locking failure for subtitle job {}, skipping (will be retried)",
                    job.getMediaFile().getMediaFileId());
        } catch (Exception e) {
            log.error("Failed to process subtitle job for MediaFile: {}",
                    job.getMediaFile().getMediaFileId(), e);

            job.setRetryCount(job.getRetryCount() + 1);
            job.setErrorMessage(truncate(e.getMessage(), 500));

            if (job.getRetryCount() >= MAX_RETRY_COUNT) {
                job.setStatus(SubtitleJobStatus.FAILED);
            } else {
                job.setStatus(SubtitleJobStatus.QUEUED);
            }

            try {
                subtitleJobRepository.save(job);
            } catch (ObjectOptimisticLockingFailureException ole) {
                log.warn("Optimistic locking failure while saving error state for job {}, skipping",
                        job.getMediaFile().getMediaFileId());
            }
        }
    }

    private void upsertSubtitle(MediaFile mediaFile, SubtitleResult subtitle) {
        Long mediaFileId = mediaFile.getId();
        String language = normalizeLanguage(subtitle.getLanguageCode());
        String format = subtitle.getFormat() == null || subtitle.getFormat().isBlank() ? "vtt" : subtitle.getFormat();

        try {
            Optional<MediaSubtitle> existing = mediaFileId == null
                    ? Optional.empty()
                    : subtitleRepository.findByMediaFileIdAndLanguageCode(mediaFileId, language);

            MediaSubtitle mediaSubtitle = existing.orElseGet(() -> MediaSubtitle.builder()
                    .mediaFile(mediaFile)
                    .languageCode(language)
                    .build());

            mediaSubtitle.setFormat(format);
            mediaSubtitle.setSubtitleContent(subtitle.getContent());
            mediaSubtitle.setOpensubtitlesId(subtitle.getOpensubtitlesId());
            subtitleRepository.save(mediaSubtitle);
        } catch (DataIntegrityViolationException e) {
            // Another job likely inserted the same (media_file_id, language_code) concurrently.
            log.info("Subtitle already persisted concurrently for MediaFile: {} language={}",
                    mediaFile.getMediaFileId(), language);
        }
    }

    private String normalizeLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "en";
        }
        return languageCode.trim().toLowerCase(Locale.ROOT);
    }

    public void queueSubtitleFetch(MediaFile mediaFile, String imdbId) {
        if (!serviceConfig.getOpensubtitles().isEnabled()) {
            return;
        }

        // Check if job already exists or subtitle already fetched
        boolean jobExists = subtitleJobRepository.existsByMediaFileIdAndStatusIn(
                mediaFile.getId(),
                Set.of(SubtitleJobStatus.QUEUED, SubtitleJobStatus.RUNNING));

        if (jobExists) {
            log.debug("Subtitle job already queued for MediaFile: {}", mediaFile.getMediaFileId());
            return;
        }

        boolean hasSubtitle = subtitleRepository.existsByMediaFileIdAndLanguageCode(
                mediaFile.getId(), "en");

        if (hasSubtitle) {
            log.debug("Subtitle already exists for MediaFile: {}", mediaFile.getMediaFileId());
            return;
        }

        SubtitleJob job = SubtitleJob.builder()
                .mediaFile(mediaFile)
                .imdbId(imdbId)
                .status(SubtitleJobStatus.QUEUED)
                .retryCount(0)
                .build();

        subtitleJobRepository.save(job);
        log.info("Queued subtitle fetch for MediaFile: {} with IMDB ID: {}",
                mediaFile.getMediaFileId(), imdbId);
    }

    /**
     * Scheduled task to check quota and re-queue jobs that were blocked due to quota limits.
     * Runs every hour to check if quota has been reset (OpenSubtitles resets daily).
     */
    @Scheduled(fixedDelay = 3600000L) // Every hour
    @SchedulerLock(name = "check-subtitle-quota-lock")
    @Transactional
    public void checkQuotaAndRequeueJobs() {
        if (!serviceConfig.getOpensubtitles().isEnabled()) {
            return;
        }

        int quotaExceededCount = subtitleJobRepository.countAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED);
        if (quotaExceededCount == 0) {
            return;
        }

        log.info("Found {} jobs blocked due to quota limits, checking if quota has been reset", quotaExceededCount);

        // Refresh quota from OpenSubtitles
        subtitleProvider.refreshQuota();

        if (subtitleProvider.hasDownloadsRemaining()) {
            log.info("Quota available ({}), re-queuing blocked jobs", subtitleProvider.getRemainingDownloads());

            List<SubtitleJob> blockedJobs = subtitleJobRepository.findAllByStatus(SubtitleJobStatus.QUOTA_EXCEEDED);
            for (SubtitleJob job : blockedJobs) {
                job.setStatus(SubtitleJobStatus.QUEUED);
                job.setErrorMessage(null);
            }
            subtitleJobRepository.saveAll(blockedJobs);

            log.info("Re-queued {} jobs", blockedJobs.size());
        } else {
            log.info("Quota still exhausted, will check again later");
        }
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.length() <= maxLength ? s : s.substring(0, maxLength);
    }
}
