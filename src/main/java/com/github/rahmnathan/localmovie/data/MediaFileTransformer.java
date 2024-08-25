package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.persistence.entity.*;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class MediaFileTransformer {

    public static MediaEventDto toMediaEventDto(MediaFileEvent mediaFileEvent) {
        return MediaEventDto.builder()
                .id(mediaFileEvent.getId())
                .event(mediaFileEvent.getEvent())
                .timestamp(mediaFileEvent.getTimestamp())
                .relativePath(mediaFileEvent.getRelativePath())
                .mediaFile(toMediaFileDto(mediaFileEvent.getMediaFile(), true))
                .build();
    }

    public static MediaFileDto toMediaFileDto(MediaFile mediaFile, boolean includeImage) {
        MediaFileDto.MediaFileDtoBuilder builder = MediaFileDto.builder();

        builder.id(mediaFile.getId());
        builder.mediaFileId(mediaFile.getMediaFileId());
        builder.fileName(mediaFile.getFileName());
        builder.absolutePath(mediaFile.getAbsolutePath());
        builder.created(mediaFile.getCreated());
        builder.updated(mediaFile.getUpdated());
        builder.path(mediaFile.getPath());

        Media media = mediaFile.getMedia();
        if(media != null) {
            MediaFileDto.MediaDto.MediaDtoBuilder mediaDto = MediaFileDto.MediaDto.builder();
            mediaDto.id(media.getId());
            mediaDto.mediaType(media.getMediaType());
            if(includeImage && media.getImage() != null && media.getImage().getImage() != null) {
                mediaDto.image(media.getImage().getImage());
            }
            mediaDto.genre(media.getGenre());
            mediaDto.title(media.getTitle());
            mediaDto.actors(media.getActors());
            mediaDto.plot(media.getPlot());
            mediaDto.metaRating(media.getMetaRating());
            mediaDto.imdbRating(media.getImdbRating());
            mediaDto.created(media.getCreated());
            mediaDto.updated(media.getUpdated());
            mediaDto.releaseYear(media.getReleaseYear());
            mediaDto.number(media.getNumber());

            builder.media(mediaDto.build());
        }

        Set<MediaView> mediaViews = mediaFile.getMediaViews();
        if(mediaViews != null && !mediaViews.isEmpty()) {
            MediaFileDto.MediaViewDto.MediaViewDtoBuilder mediaViewDto = MediaFileDto.MediaViewDto.builder();
            MediaView mediaView = mediaViews.iterator().next();

            mediaViewDto.id(mediaView.getId());
            mediaViewDto.position(mediaView.getPosition());
            mediaViewDto.created(mediaView.getCreated());
            mediaViewDto.updated(mediaView.getUpdated());

            MediaUser mediaUser = mediaView.getMediaUser();
            if(mediaUser != null) {
                MediaFileDto.MediaUserDto.MediaUserDtoBuilder mediaUserDto = MediaFileDto.MediaUserDto.builder();
                mediaUserDto.id(mediaUser.getId());
                mediaUserDto.created(mediaUser.getCreated());
                mediaUserDto.updated(mediaUser.getUpdated());
                mediaUserDto.userId(mediaUser.getUserId());

                mediaViewDto.mediaUser(mediaUserDto.build());
            }

            builder.mediaViews(Set.of(mediaViewDto.build()));
        }


        return builder.build();
    }
}
