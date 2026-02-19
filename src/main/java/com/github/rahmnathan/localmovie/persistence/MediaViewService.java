package com.github.rahmnathan.localmovie.persistence;

import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaUser;
import com.github.rahmnathan.localmovie.persistence.entity.MediaView;
import com.github.rahmnathan.localmovie.persistence.entity.QMediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaViewRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QMediaFile.mediaFile.mediaViews.any().mediaUser.userId.eq(securityUtils.getUsername()));
        predicates.add(QMediaFile.mediaFile.mediaViews.any().updated.after(LocalDateTime.now().minusMonths(6)));

        return jpaQuery.from(qMediaFile)
                .where(predicates.toArray(new Predicate[0]))
                .fetchCount();
    }

    public List<MediaFile> getHistory(MediaRequest request) {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QMediaFile.mediaFile.mediaViews.any().mediaUser.userId.eq(securityUtils.getUsername()));
        predicates.add(QMediaFile.mediaFile.mediaViews.any().updated.after(LocalDateTime.now().minusMonths(3)));

        OrderSpecifier<LocalDateTime> orderSpecifier = qMediaFile.mediaViews.any().updated.desc();

        return executeHistoryQuery(request, jpaQuery, qMediaFile, predicates, orderSpecifier);
    }

    private List<MediaFile> executeHistoryQuery(MediaRequest request, JPAQuery<MediaFile> jpaQuery, QMediaFile qMediaFile,
                                                 List<Predicate> predicates, OrderSpecifier<?> orderSpecifier) {
        OrderSpecifier<String> secondaryOrder = qMediaFile.mediaFileId.asc();

        List<String> ids = jpaQuery.from(qMediaFile)
                .select(qMediaFile.mediaFileId)
                .orderBy(orderSpecifier, secondaryOrder)
                .where(predicates.toArray(new Predicate[0]))
                .offset((long) request.getPage() * request.getPageSize())
                .limit(request.getPageSize())
                .fetch();

        log.info("Found {} history ids", ids.size());

        jpaQuery = new JPAQuery<>(entityManager);
        qMediaFile = QMediaFile.mediaFile;

        return jpaQuery.from(qMediaFile)
                .where(qMediaFile.mediaFileId.in(ids))
                .leftJoin(QMediaFile.mediaFile.media).fetchJoin()
                .leftJoin(QMediaFile.mediaFile.parent).fetchJoin()
                .orderBy(orderSpecifier, secondaryOrder)
                .fetch();
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
