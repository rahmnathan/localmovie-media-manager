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
    private final MediaFileRepository fileRepository;
    private final MediaViewRepository mediaViewRepository;
    private final MediaUserRepository userRepository;
    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void addView(String id, Double position, Double duration) {
        Optional<MediaFile> mediaFileOptional = fileRepository.findByMediaFileId(id);
        if (mediaFileOptional.isEmpty()) return;

        MediaFile mediaFile = mediaFileOptional.get();
        String userName = securityUtils.getUsername();
        log.info("Adding view for User: {} Path: {} Position: {} Duration: {}", userName, mediaFile.getPath(), position, duration);

        if (mediaFile.getMediaViews().isEmpty()) {
            MediaUser mediaUser = userRepository.findByUserId(userName).orElse(new MediaUser(userName));
            MediaView mediaView = new MediaView(mediaFile, mediaUser, position, duration);
            mediaFile.addMediaView(mediaView);
            mediaUser.addMediaView(mediaView);
            userRepository.save(mediaUser);
            mediaViewRepository.save(mediaView);
        } else {
            MediaView mediaView = mediaFile.getMediaViews().iterator().next();
            mediaView.setPosition(position);
            if (duration != null && duration > 0) {
                mediaView.setDuration(duration);
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
                .leftJoin(QMediaFile.mediaFile.mediaViews).fetchJoin()
                .leftJoin(QMediaFile.mediaFile.parent).fetchJoin()
                .orderBy(orderSpecifier, secondaryOrder)
                .fetch();
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
