package com.github.rahmnathan.localmovie.persistence;

import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaUser;
import com.github.rahmnathan.localmovie.persistence.entity.MediaView;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaViewService {
    private static final double MILLISECONDS_TO_SECONDS = 1000.0;
    private static final double ASSUME_MILLISECONDS_THRESHOLD = 100_000.0;

    private final MediaFileRepository fileRepository;
    private final MediaViewRepository mediaViewRepository;
    private final MediaUserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void addView(String id, Double position, Double duration) {
        addView(id, securityUtils.getUsername(), position, duration);
    }

    @Transactional
    public void addView(String id, String userId, Double position, Double duration) {
        Optional<MediaFile> mediaFileOptional = fileRepository.findByMediaFileId(id);
        if (mediaFileOptional.isEmpty()) return;

        MediaFile mediaFile = mediaFileOptional.get();
        String userName = userId != null ? userId : securityUtils.getUsername();
        Double normalizedPosition = normalizeToSeconds(position);
        Double normalizedDuration = normalizeToSeconds(duration);
        log.info("Adding view for User: {} Path: {} Position: {} Duration: {}", userName, mediaFile.getPath(), normalizedPosition, normalizedDuration);

        // Find existing view for this user
        Optional<MediaView> existingView = mediaFile.getMediaViews().stream()
                .filter(v -> v.getMediaUser().getUserId().equals(userName))
                .findFirst();

        if (existingView.isEmpty()) {
            MediaUser mediaUser = userRepository.findByUserId(userName).orElse(new MediaUser(userName));
            MediaView mediaView = new MediaView(mediaFile, mediaUser, normalizedPosition, normalizedDuration);
            mediaFile.addMediaView(mediaView);
            mediaUser.addMediaView(mediaView);
            userRepository.save(mediaUser);
            mediaViewRepository.save(mediaView);
        } else {
            MediaView mediaView = existingView.get();
            mediaView.setPosition(normalizedPosition);
            if (normalizedDuration != null && normalizedDuration > 0) {
                mediaView.setDuration(normalizedDuration);
            }
        }

        fileRepository.save(mediaFile);
    }

    public long countHistory() {
        return mediaViewRepository.countRecentByUserId(securityUtils.getUsername(), LocalDateTime.now().minusMonths(6));
    }

    public List<MediaFile> getHistory(MediaRequest request) {
        List<String> ids = mediaViewRepository.findRecentMediaFileIdsByUserId(
                securityUtils.getUsername(),
                LocalDateTime.now().minusMonths(3),
                PageRequest.of(request.getPage(), request.getPageSize())
        );

        log.info("Found {} history ids", ids.size());

        if (ids.isEmpty()) {
            return List.of();
        }

        List<MediaFile> files = fileRepository.findByMediaFileIdInWithMediaAndParent(ids);
        Map<String, MediaFile> byMediaFileId = new HashMap<>(files.size());
        for (MediaFile file : files) {
            byMediaFileId.put(file.getMediaFileId(), file);
        }

        // Preserve history ordering by most recently viewed first.
        return ids.stream()
                .map(byMediaFileId::get)
                .filter(java.util.Objects::nonNull)
                .toList();

    }

    private Double normalizeToSeconds(Double value) {
        if (value == null || value <= 0) {
            return value;
        }
        if (value > ASSUME_MILLISECONDS_THRESHOLD) {
            return value / MILLISECONDS_TO_SECONDS;
        }
        return value;
    }

    @Transactional
    public void clearHistory() {
        String username = securityUtils.getUsername();
        log.info("Clearing history for user: {}", username);
        mediaViewRepository.deleteAllByUserId(username);
    }

    @Transactional
    public void removeFromHistory(String mediaFileId) {
        String username = securityUtils.getUsername();
        log.info("Removing {} from history for user: {}", mediaFileId, username);
        mediaViewRepository.deleteByMediaFileIdAndUserId(mediaFileId, username);
    }
}
