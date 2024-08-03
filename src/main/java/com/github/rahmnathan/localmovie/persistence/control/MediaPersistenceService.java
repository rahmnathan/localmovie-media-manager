package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.MediaFileTransformer;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.persistence.entity.*;
import com.github.rahmnathan.localmovie.persistence.entity.QMediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.*;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static com.github.rahmnathan.localmovie.data.MediaOrder.SEASONS_EPISODES;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class MediaPersistenceService {
    private final MediaFileEventRepository eventRepository;
    private final MediaViewRepository mediaViewRepository;
    private final MediaFileRepository fileRepository;
    private final MediaUserRepository userRepository;
    private final MediaImageRepository mediaImageRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public Optional<MediaFile> getMediaFileByPath(String path) {
        return fileRepository.findByPath(path);
    }

    @Transactional
    public void saveEvent(MediaFileEvent event){
        eventRepository.save(event);
    }

    @Transactional
    public void deleteAllByRelativePath(String path) {
        eventRepository.deleteAllByRelativePath(path);
        fileRepository.deleteByPath(path);
    }

    public Optional<String> getMediaFilePathById(String id) {
        return fileRepository.getAbsolutePathById(id);
    }

    public Optional<MediaFile> getMediaFileByIdWithViews(String id) {
        return fileRepository.findByIdWithViews(id, getUsername());
    }

    public byte[] getMediaImage(String path){
        return mediaImageRepository.getImageByPath(path);
    }

    public byte[] getMediaImageById(String id){
        return mediaImageRepository.getImageById(id);
    }

    public boolean existsByPath(String path){
        return fileRepository.existsByPath(path);
    }

    public List<MediaFileDto> getMediaHistory(MediaRequest request, boolean includePoster) {
        return getHistory(request).stream()
                .map(mediaFile -> MediaFileTransformer.toMediaFileDto(mediaFile, includePoster))
                .toList();
    }

    public long countHistory() {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QMediaFile.mediaFile.mediaViews.any().mediaUser.userId.eq(getUsername()));
        predicates.add(QMediaFile.mediaFile.mediaViews.any().updated.after(LocalDateTime.now().minusMonths(6)));

        return jpaQuery.from(qMediaFile)
                .where(predicates.toArray(new Predicate[0]))
                .fetchCount();
    }

    public List<MediaFile> getHistory(MediaRequest request) {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QMediaFile.mediaFile.mediaViews.any().mediaUser.userId.eq(getUsername()));
        predicates.add(QMediaFile.mediaFile.mediaViews.any().updated.after(LocalDateTime.now().minusMonths(6)));

        OrderSpecifier<LocalDateTime> orderSpecifier = qMediaFile.mediaViews.any().updated.desc();

        List<String> ids = jpaQuery.from(qMediaFile)
                .select(qMediaFile.mediaFileId)
                .orderBy(orderSpecifier)
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
                .leftJoin(QMediaFile.mediaFile.mediaViews).fetchJoin()
                .orderBy(orderSpecifier)
                .fetch();
    }

    public long count(MediaRequest request) {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(qMediaFile.parentPath.eq(request.getPath()));

        if(StringUtils.hasText(request.getGenre())){
            predicates.add(qMediaFile.media.genre.containsIgnoreCase(request.getGenre()));
        }

        if(StringUtils.hasText(request.getQ())) {
            predicates.add(qMediaFile.media.genre.containsIgnoreCase(request.getQ())
                    .or(qMediaFile.media.title.containsIgnoreCase(request.getQ()))
                    .or(qMediaFile.media.actors.containsIgnoreCase(request.getQ())));
        }

        return jpaQuery.from(qMediaFile)
                .where(predicates.toArray(new Predicate[0]))
                .fetchCount();
    }

    public List<MediaFileDto> getMediaFiles(MediaRequest request, boolean includePoster) {
        return getMediaFiles(request).stream()
                .map(mediaFile -> MediaFileTransformer.toMediaFileDto(mediaFile, includePoster))
                .toList();
    }

    public List<MediaFile> getMediaFiles(MediaRequest request) {
        JPAQuery<MediaFile> jpaQuery = new JPAQuery<>(entityManager);
        QMediaFile qMediaFile = QMediaFile.mediaFile;

        List<com.querydsl.core.types.Predicate> predicates = new ArrayList<>();
        predicates.add(qMediaFile.parentPath.eq(request.getPath()));

        if(StringUtils.hasText(request.getGenre())){
            predicates.add(qMediaFile.media.genre.containsIgnoreCase(request.getGenre()));
        }

        if(StringUtils.hasText(request.getQ())) {
            predicates.add(qMediaFile.media.genre.containsIgnoreCase(request.getQ())
                    .or(qMediaFile.media.title.containsIgnoreCase(request.getQ()))
                    .or(qMediaFile.media.actors.containsIgnoreCase(request.getQ())));
        }

        OrderSpecifier orderSpecifier = qMediaFile.fileName.asc();
        if (request.getPath().split(File.separator).length > 1) {
            orderSpecifier = SEASONS_EPISODES.getOrderSpecifier();
        } else if (StringUtils.hasText(request.getOrder())) {
            orderSpecifier = MediaOrder.lookup(request.getOrder()).getOrderSpecifier();
        }

        List<String> ids = jpaQuery.from(qMediaFile)
                .select(qMediaFile.mediaFileId)
                .orderBy(orderSpecifier)
                .where(predicates.toArray(new Predicate[predicates.size()]))
                .offset((long) request.getPage() * request.getPageSize())
                .limit(request.getPageSize())
                .fetch();

        log.info("Found {} ids", ids.size());

        jpaQuery = new JPAQuery<>(entityManager);
        qMediaFile = QMediaFile.mediaFile;

        return jpaQuery.from(qMediaFile)
                .where(qMediaFile.mediaFileId.in(ids))
                .leftJoin(QMediaFile.mediaFile.media).fetchJoin()
                .leftJoin(QMediaFile.mediaFile.mediaViews).fetchJoin()
                .orderBy(orderSpecifier)
                .fetch();
    }

    @Transactional
    public void addView(String id, Double position) {

        Optional<MediaFile> mediaFileOptional = fileRepository.findByMediaFileId(id);
        if(mediaFileOptional.isEmpty()) return;

        MediaFile mediaFile = mediaFileOptional.get();
        String userName = getUsername();
        log.info("Adding view for User: {} Path: {} Position: {}", userName, mediaFile.getPath(), position);
        if(mediaFile.getMediaViews().isEmpty()){
            MediaUser mediaUser = userRepository.findByUserId(userName).orElse(new MediaUser(userName));
            MediaView mediaView = new MediaView(mediaFile, mediaUser, position);
            mediaFile.addMediaView(mediaView);
            mediaUser.addMediaView(mediaView);
            userRepository.save(mediaUser);
            mediaViewRepository.save(mediaView);
        } else {
            MediaView mediaView = mediaFile.getMediaViews().iterator().next();
            mediaView.setPosition(position);
        }

        fileRepository.save(mediaFile);
    }

    public Long getMediaFileEventCount(LocalDateTime localDateTime) {
        return eventRepository.countAllByTimestampAfter(localDateTime);
    }

    public List<MediaFileEvent> getMediaFileEvents(LocalDateTime localDateTime, Pageable pageable) {
        return eventRepository.findAllByTimestampAfterOrderByTimestamp(localDateTime, pageable);
    }

    private String getUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null){
            return authentication.getName();
        }

        return "movieuser";
    }
}
