package com.github.rahmnathan.localmovie.persistence.control;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.persistence.entity.MediaUser;
import com.github.rahmnathan.localmovie.persistence.entity.MediaView;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileEventRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaViewRepository;
import com.github.rahmnathan.localmovie.web.data.MediaRequest;
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

import static com.github.rahmnathan.localmovie.control.MediaDirectoryMonitor.ROOT_MEDIA_FOLDER;
import static com.github.rahmnathan.localmovie.data.MediaOrder.SEASONS_EPISODES;

@Service
@Transactional
public class MediaPersistenceService {
    private final MediaFileEventRepository mediaFileEventRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MediaUserRepository userRepository;
    private final MediaViewRepository viewRepository;

    public MediaPersistenceService(MediaFileEventRepository mediaFileEventRepository, MediaFileRepository mediaFileRepository,
                                   MediaViewRepository viewRepository, MediaUserRepository userRepository) {
        this.mediaFileEventRepository = mediaFileEventRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.userRepository = userRepository;
        this.viewRepository = viewRepository;
    }

    public byte[] getMediaImage(String path){
        return mediaFileRepository.findMediaPosterByPath(path);
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

        String userName = getUsername();
        Pageable pageable = PageRequest.of(request.getPage(), request.getResultsPerPage(), sort);
        return mediaFileRepository.findAllByParentPath(request.getPath(), userName);
    }

    public void addView(String path, long position) {
        String userName = getUsername();
        String relativePath = path.split(ROOT_MEDIA_FOLDER)[1];
        MediaFile mediaFile = mediaFileRepository.findByPath(relativePath, userName);
        if(mediaFile.getMediaViews().isEmpty()){
            MediaUser mediaUser = new MediaUser(userName);
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
