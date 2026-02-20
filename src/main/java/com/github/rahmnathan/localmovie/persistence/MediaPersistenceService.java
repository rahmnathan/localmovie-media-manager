package com.github.rahmnathan.localmovie.persistence;

import com.github.rahmnathan.localmovie.data.*;
import com.github.rahmnathan.localmovie.data.transformer.MediaFileTransformer;
import com.github.rahmnathan.localmovie.persistence.entity.*;
import com.github.rahmnathan.localmovie.persistence.entity.QMediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.*;
import com.google.common.annotations.VisibleForTesting;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static com.github.rahmnathan.localmovie.data.MediaOrder.SEASONS_EPISODES;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaPersistenceService {
    private final MediaFileEventRepository eventRepository;
    private final MediaFileRepository fileRepository;
    private final MediaImageRepository mediaImageRepository;
    private final MediaViewService mediaViewService;
    private final MediaViewRepository mediaViewRepository;
    private final MediaFavoriteService mediaFavoriteService;
    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

    // ========== Media File CRUD Operations ==========

    public Optional<MediaFile> getMediaFileByPath(MediaPath path) {
        return fileRepository.findByPath(path.getRelativePath());
    }

    @Transactional
    public MediaFile saveMediaFile(MediaFile mediaFile) {
        return fileRepository.save(mediaFile);
    }

    @Transactional
    public void saveEvent(MediaFileEvent event) {
        eventRepository.save(event);
    }

    @Transactional
    public void deleteAllByRelativePath(String path) {
        eventRepository.deleteAllByRelativePath(path);
        fileRepository.deleteByPath(path);
    }

    public Optional<MediaFile> findByMediaFileId(String id) {
        return fileRepository.findByMediaFileId(id);
    }

    public Optional<MediaFile> getMediaFileByIdWithViews(String id) {
        return fileRepository.findByIdWithViews(id, securityUtils.getUsername());
    }

    public Optional<MediaFileDto> getMediaFileDtoById(String id) {
        Optional<MediaFile> mediaFile = fileRepository.findByMediaFileId(id);
        if (mediaFile.isEmpty()) {
            return Optional.empty();
        }

        Map<String, MediaView> userViews = findCurrentUserViewsByMediaFileIds(List.of(id));
        return Optional.of(MediaFileTransformer.toMediaFileDto(mediaFile.get(), userViews.get(id)));
    }

    public boolean existsByPath(String path) {
        return fileRepository.existsByPath(path);
    }

    // ========== Media Image Operations ==========

    public byte[] getMediaImage(String path) {
        return mediaImageRepository.getImageByPath(path);
    }

    public byte[] getMediaImageById(String id) {
        return mediaImageRepository.getImageById(id);
    }

    // ========== Media Events ==========

    public List<MediaFileEvent> getMediaFileEvents(LocalDateTime localDateTime, Pageable pageable) {
        return eventRepository.findAllByTimestampAfterOrderByTimestampAsc(localDateTime, pageable);
    }

    // ========== View/History Operations (delegated) ==========

    @Transactional
    public void addView(String id, Double position, Double duration) {
        mediaViewService.addView(id, position, duration);
    }

    @Transactional
    public void addView(String id, String userId, Double position, Double duration) {
        mediaViewService.addView(id, userId, position, duration);
    }

    public long countHistory() {
        return mediaViewService.countHistory();
    }

    public List<MediaFile> getHistory(MediaRequest request) {
        return mediaViewService.getHistory(request);
    }

    // ========== Favorite Operations (delegated) ==========

    @Transactional
    public void addFavorite(String id) {
        mediaFavoriteService.addFavorite(id);
    }

    @Transactional
    public void removeFavorite(String id) {
        mediaFavoriteService.removeFavorite(id);
    }

    public boolean isFavorite(String id) {
        return mediaFavoriteService.isFavorite(id);
    }

    public long countFavorites() {
        return mediaFavoriteService.countFavorites();
    }

    public List<MediaFile> getFavorites(MediaRequest request) {
        return mediaFavoriteService.getFavorites(request);
    }

    // ========== Media Query Operations ==========

    public long countMediaFiles(MediaRequest request) {
        MediaRequestType mediaRequestType = MediaRequestType.lookup(request.getType()).orElse(null);
        if (mediaRequestType == MediaRequestType.HISTORY) {
            return countHistory();
        }
        if (mediaRequestType == MediaRequestType.FAVORITES) {
            return countFavorites();
        }

        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = extractPredicates(request, qMediaFile, mediaRequestType);

        return jpaQuery.from(qMediaFile)
                .where(predicates.toArray(new Predicate[0]))
                .fetchCount();
    }

    public List<MediaFileDto> getMediaFileDtos(MediaRequest request) {
        List<MediaFile> mediaFiles = getMediaFiles(request);
        Set<String> mediaFileIds = mediaFiles.stream().map(MediaFile::getMediaFileId).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<String> favoriteIds = mediaFavoriteService.findFavoriteIdsForCurrentUser(mediaFileIds);
        Map<String, MediaView> userViews = findCurrentUserViewsByMediaFileIds(
                mediaFiles.stream().map(MediaFile::getMediaFileId).toList()
        );
        boolean includeDetails = request.getIncludeDetails() == null || request.getIncludeDetails();

        return mediaFiles.stream()
                .map(mediaFile -> {
                    MediaFileDto dto = MediaFileTransformer.toMediaFileDto(
                            mediaFile,
                            userViews.get(mediaFile.getMediaFileId()),
                            includeDetails
                    );
                    dto.setFavorite(favoriteIds.contains(mediaFile.getMediaFileId()));
                    return dto;
                })
                .toList();
    }

    public Map<String, MediaView> findCurrentUserViewsByMediaFileIds(List<String> mediaFileIds) {
        if (mediaFileIds == null || mediaFileIds.isEmpty()) {
            return Map.of();
        }

        return mediaViewRepository.findByMediaFileIdsAndUserId(mediaFileIds, securityUtils.getUsername())
                .stream()
                .collect(HashMap::new, (map, view) -> map.put(view.getMediaFile().getMediaFileId(), view), HashMap::putAll);
    }

    @VisibleForTesting
    List<MediaFile> getMediaFiles(MediaRequest request) {
        MediaRequestType mediaRequestType = MediaRequestType.lookup(request.getType()).orElse(null);
        if (mediaRequestType == MediaRequestType.HISTORY) {
            return getHistory(request);
        }
        if (mediaRequestType == MediaRequestType.FAVORITES) {
            return getFavorites(request);
        }

        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = extractPredicates(request, qMediaFile, mediaRequestType);

        OrderSpecifier<?> orderSpecifier;
        if ((request.getPath() != null && request.getPath().split(File.separator).length > 1)
                || mediaRequestType == MediaRequestType.SEASONS
                || mediaRequestType == MediaRequestType.EPISODES) {
            orderSpecifier = SEASONS_EPISODES.getOrderSpecifier();
        } else if (StringUtils.hasText(request.getOrder())) {
            orderSpecifier = MediaOrder.lookup(request.getOrder()).getOrderSpecifier();
        } else {
            orderSpecifier = qMediaFile.fileName.asc();
        }

        return executeQuery(request, jpaQuery, qMediaFile, predicates, orderSpecifier);
    }

    private List<Predicate> extractPredicates(MediaRequest request, QMediaFile qMediaFile, MediaRequestType mediaRequestType) {
        List<Predicate> predicates = new ArrayList<>();

        if (mediaRequestType != null && mediaRequestType.getType() != null) {
            predicates.add(qMediaFile.mediaFileType.eq(mediaRequestType.getType()));
        }

        if (StringUtils.hasText(request.getParentId())) {
            predicates.add(qMediaFile.parent.mediaFileId.eq(request.getParentId()));
        } else if (StringUtils.hasText(request.getPath())) {
            predicates.add(qMediaFile.parentPath.eq(request.getPath()));
        }

        if (StringUtils.hasText(request.getGenre())) {
            predicates.add(qMediaFile.media.genre.containsIgnoreCase(request.getGenre()));
        }

        if (StringUtils.hasText(request.getQ())) {
            predicates.add(qMediaFile.media.genre.containsIgnoreCase(request.getQ())
                    .or(qMediaFile.media.title.containsIgnoreCase(request.getQ()))
                    .or(qMediaFile.media.actors.containsIgnoreCase(request.getQ())));
        }

        return predicates;
    }

    private List<MediaFile> executeQuery(MediaRequest request, JPAQuery<MediaFile> jpaQuery, QMediaFile qMediaFile,
                                          List<Predicate> predicates, OrderSpecifier<?> orderSpecifier) {
        // Secondary sort by mediaFileId ensures deterministic ordering for pagination
        OrderSpecifier<String> secondaryOrder = qMediaFile.mediaFileId.asc();

        List<String> ids = jpaQuery.from(qMediaFile)
                .select(qMediaFile.mediaFileId)
                .orderBy(orderSpecifier, secondaryOrder)
                .where(predicates.toArray(new Predicate[0]))
                .offset((long) request.getPage() * request.getPageSize())
                .limit(request.getPageSize())
                .fetch();

        log.info("Found {} ids", ids.size());

        jpaQuery = new JPAQuery<>(entityManager);
        qMediaFile = QMediaFile.mediaFile;

        return jpaQuery.from(qMediaFile)
                .where(qMediaFile.mediaFileId.in(ids))
                .leftJoin(QMediaFile.mediaFile.media).fetchJoin()
                .leftJoin(QMediaFile.mediaFile.parent).fetchJoin()
                .orderBy(orderSpecifier, secondaryOrder)
                .fetch();
    }
}
