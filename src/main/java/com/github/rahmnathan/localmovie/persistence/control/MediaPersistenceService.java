package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.persistence.entity.*;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileEventRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
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

import static com.github.rahmnathan.localmovie.control.MediaDirectoryMonitor.ROOT_MEDIA_FOLDER;
import static com.github.rahmnathan.localmovie.data.MediaOrder.SEASONS_EPISODES;

@Service
@Transactional
@AllArgsConstructor
public class MediaPersistenceService {
    private final Logger logger = LoggerFactory.getLogger(MediaPersistenceService.class);
    private final MediaFileEventRepository mediaFileEventRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MediaUserRepository mediaUserRepository;
    private final MediaRepository mediaRepository;

    public byte[] getMediaImage(String path){
        return mediaRepository.getImageByPath(path);
    }

    public boolean existsByPath(String path){
        return mediaFileRepository.existsByPath(path);
    }

    public List<MediaFile> getMediaFilesByParentPath(MediaRequest request) {
        Sort sort;
        if (request.getPath().split(File.separator).length > 1) {
            sort = SEASONS_EPISODES.getSort();
        } else {
            sort = request.getOrder().getSort();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getResultsPerPage(), sort);
        return mediaFileRepository.findAllByParentPath(request.getPath(), getUsername(), pageable);
    }

    public List<RedactedMediaFile> getMediaFilesByParentPathNoPoster(MediaRequest request) {
        Sort sort;
        if (request.getPath().split(File.separator).length > 1) {
            sort = SEASONS_EPISODES.getSort();
        } else {
            sort = request.getOrder().getSort();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getResultsPerPage(), sort);
        return mediaFileRepository.findAllByParentPathNoPoster(request.getPath(), getUsername(), pageable);
    }

    public void addView(String path, long position) {
        String userName = getUsername();
        logger.info("Adding view for User: {} Path: {} Position: {}", userName, path, position);
        String relativePath = path.split(ROOT_MEDIA_FOLDER)[1];
        MediaFile mediaFile = mediaFileRepository.findByPath(relativePath, userName);
        if(mediaFile.getMediaViews().isEmpty()){
            MediaUser mediaUser = mediaUserRepository.findByUserId(userName).orElse(new MediaUser(userName));
            MediaView mediaView = new MediaView(mediaFile, mediaUser, position);
            mediaFile.addMediaView(mediaView);
            mediaUser.addMediaView(mediaView);
        } else {
            MediaView mediaView = mediaFile.getMediaViews().iterator().next();
            mediaView.setPosition(position);
        }

        mediaFileRepository.save(mediaFile);
    }

    public List<MediaFileEvent> getMediaFileEvents(LocalDateTime localDateTime) {
        return mediaFileEventRepository.findAllByTimestampAfterOrderByTimestamp(localDateTime);
    }

    public long countMediaFiles(String path){
        return mediaFileRepository.countAllByParentPath(path);
    }

    private String getUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null){
            return authentication.getPrincipal().toString();
        }

        return "movieuser";
    }
}
