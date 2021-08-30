package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.persistence.entity.*;
import com.github.rahmnathan.localmovie.persistence.repository.*;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final MediaRepository mediaRepository;

    public Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> getMediaFileByPath(String path) {
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

    public Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> getMediaFileById(String id) {
        return fileRepository.findByMediaFileId(id);
    }

    public Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> getMediaFileByIdWithViews(String id) {
        return fileRepository.findByIdWithViews(id, getUsername());
    }

    public byte[] getMediaImage(String path){
        return mediaRepository.getImageByPath(path);
    }

    public byte[] getMediaImageById(String id){
        return fileRepository.getImageById(id);
    }

    public boolean existsByPath(String path){
        return fileRepository.existsByPath(path);
    }

    public List<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> getMediaFilesByParentPath(MediaRequest request) {
        Sort sort;
        if (request.getPath().split(File.separator).length > 1) {
            sort = SEASONS_EPISODES.getSort();
        } else {
            sort = request.getOrder().getSort();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getResultsPerPage(), sort);
        return fileRepository.findAllByParentPath(request.getPath(), getUsername(), pageable);
    }

    public List<MediaFileNoPoster> getMediaFilesByParentPathNoPoster(MediaRequest request) {
        Sort sort;
        if (request.getPath().split(File.separator).length > 1) {
            sort = SEASONS_EPISODES.getSort();
        } else {
            sort = request.getOrder().getSort();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getResultsPerPage(), sort);
        return fileRepository.findAllByParentPathNoPoster(request.getPath(), getUsername(), pageable);
    }

    @Transactional
    public void addView(String id, Double position) {

        Optional<com.github.rahmnathan.localmovie.persistence.entity.MediaFile> mediaFileOptional = fileRepository.findByMediaFileId(id);
        if(mediaFileOptional.isEmpty()) return;

        com.github.rahmnathan.localmovie.persistence.entity.MediaFile mediaFile = mediaFileOptional.get();
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

    public long countMediaFiles(String path){
        return fileRepository.countAllByParentPath(path);
    }

    private String getUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null){
            return authentication.getPrincipal().toString();
        }

        return "movieuser";
    }
}
