package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.persistence.entity.*;
import com.github.rahmnathan.localmovie.persistence.repository.*;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.github.rahmnathan.localmovie.control.MediaDatabaseInitializer.ROOT_MEDIA_FOLDER;
import static com.github.rahmnathan.localmovie.data.MediaOrder.SEASONS_EPISODES;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class MediaPersistenceService {
    private final Logger logger = LoggerFactory.getLogger(MediaPersistenceService.class);
    private final MediaFileEventRepository eventRepository;
    private final MediaViewRepository mediaViewRepository;
    private final MediaFileRepository fileRepository;
    private final MediaUserRepository userRepository;
    private final MediaRepository mediaRepository;

    public Optional<MediaFile> getMediaFileByPath(String path) {
        return fileRepository.findByPath(path);
    }

    public void delete(Media media) {
        mediaRepository.delete(media);
    }

    @Transactional
    public void saveEvent(MediaFileEvent event, MediaFile mediaFile){
        eventRepository.save(event);
        fileRepository.save(mediaFile);
    }

    @Transactional
    public void saveEvent(MediaFileEvent event){
        eventRepository.save(event);
    }

    @Transactional
    public void saveMediaFile(MediaFile mediaFile) {
        fileRepository.save(mediaFile);
    }

    @Transactional
    public void deleteAllByRelativePath(String path) {
        eventRepository.deleteAllByRelativePath(path);
        fileRepository.deleteByPath(path);
    }

    public byte[] getMediaImage(String path){
        return mediaRepository.getImageByPath(path);
    }

    public boolean existsByPath(String path){
        return fileRepository.existsByPath(path);
    }

    public List<MediaFile> getMediaFilesByParentPath(MediaRequest request) {
        Sort sort;
        if (request.getPath().split(File.separator).length > 1) {
            sort = SEASONS_EPISODES.getSort();
        } else {
            sort = request.getOrder().getSort();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getResultsPerPage(), sort);
        return fileRepository.findAllByParentPath(request.getPath(), getUsername(), pageable);
    }

    public List<RedactedMediaFile> getMediaFilesByParentPathNoPoster(MediaRequest request) {
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
    public void addView(String path, long position) {
        String userName = getUsername();
        logger.info("Adding view for User: {} Path: {} Position: {}", userName, path, position);
        String relativePath = path.split(ROOT_MEDIA_FOLDER)[1];
        MediaFile mediaFile = fileRepository.findByPath(relativePath, userName);
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
