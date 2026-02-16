package com.github.rahmnathan.localmovie.persistence;

import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFavorite;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaUser;
import com.github.rahmnathan.localmovie.persistence.entity.QMediaFavorite;
import com.github.rahmnathan.localmovie.persistence.entity.QMediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFavoriteRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaFavoriteService {
    private final MediaFavoriteRepository mediaFavoriteRepository;
    private final MediaFileRepository fileRepository;
    private final MediaUserRepository userRepository;
    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void addFavorite(String id) {
        Optional<MediaFile> mediaFileOptional = fileRepository.findByMediaFileId(id);
        if (mediaFileOptional.isEmpty()) return;

        MediaFile mediaFile = mediaFileOptional.get();
        String userName = securityUtils.getUsername();
        log.info("Adding favorite for User: {} MediaFileId: {}", userName, id);

        if (!mediaFavoriteRepository.existsByMediaFileMediaFileIdAndMediaUserUserId(id, userName)) {
            MediaUser mediaUser = userRepository.findByUserId(userName).orElse(new MediaUser(userName));
            userRepository.save(mediaUser);
            MediaFavorite mediaFavorite = new MediaFavorite(mediaFile, mediaUser);
            mediaFavoriteRepository.save(mediaFavorite);
        }
    }

    @Transactional
    public void removeFavorite(String id) {
        String userName = securityUtils.getUsername();
        log.info("Removing favorite for User: {} MediaFileId: {}", userName, id);
        mediaFavoriteRepository.deleteByMediaFileMediaFileIdAndMediaUserUserId(id, userName);
    }

    public boolean isFavorite(String id) {
        return mediaFavoriteRepository.existsByMediaFileMediaFileIdAndMediaUserUserId(id, securityUtils.getUsername());
    }

    public Set<String> findAllFavoriteIdsForCurrentUser() {
        return mediaFavoriteRepository.findAllMediaFileIdsByUserId(securityUtils.getUsername());
    }

    public long countFavorites() {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;
        QMediaFavorite qMediaFavorite = QMediaFavorite.mediaFavorite;

        return jpaQuery.from(qMediaFile)
                .innerJoin(qMediaFavorite).on(qMediaFavorite.mediaFile.eq(qMediaFile))
                .where(qMediaFavorite.mediaUser.userId.eq(securityUtils.getUsername()))
                .fetchCount();
    }

    public List<MediaFile> getFavorites(MediaRequest request) {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;
        QMediaFavorite qMediaFavorite = QMediaFavorite.mediaFavorite;

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(qMediaFavorite.mediaUser.userId.eq(securityUtils.getUsername()));

        OrderSpecifier<LocalDateTime> orderSpecifier = qMediaFavorite.created.desc();

        List<String> ids = jpaQuery.from(qMediaFile)
                .innerJoin(qMediaFavorite).on(qMediaFavorite.mediaFile.eq(qMediaFile))
                .select(qMediaFile.mediaFileId)
                .orderBy(orderSpecifier)
                .where(predicates.toArray(new Predicate[0]))
                .offset((long) request.getPage() * request.getPageSize())
                .limit(request.getPageSize())
                .fetch();

        log.info("Found {} favorite ids", ids.size());

        jpaQuery = new JPAQuery<>(entityManager);
        qMediaFile = QMediaFile.mediaFile;

        return jpaQuery.from(qMediaFile)
                .where(qMediaFile.mediaFileId.in(ids))
                .leftJoin(QMediaFile.mediaFile.media).fetchJoin()
                .leftJoin(QMediaFile.mediaFile.mediaViews).fetchJoin()
                .leftJoin(QMediaFile.mediaFile.parent).fetchJoin()
                .orderBy(qMediaFile.fileName.asc())
                .fetch();
    }
}
