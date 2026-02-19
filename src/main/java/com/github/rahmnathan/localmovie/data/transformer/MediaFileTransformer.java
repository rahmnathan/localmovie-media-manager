package com.github.rahmnathan.localmovie.data.transformer;

import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.persistence.entity.*;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class MediaFileTransformer {

    public static MediaFileDto toMediaFileDto(MediaFile mediaFile, MediaView selectedView) {
        MediaFileDto.MediaFileDtoBuilder builder = MediaFileDto.builder();

        builder.id(mediaFile.getId());
        builder.mediaFileId(mediaFile.getMediaFileId());
        builder.fileName(mediaFile.getFileName());
        builder.absolutePath(mediaFile.getAbsolutePath());
        builder.created(mediaFile.getCreated());
        builder.updated(mediaFile.getUpdated());
        builder.path(mediaFile.getPath());
        builder.streamable(mediaFile.getStreamable());
        builder.mediaFileType(mediaFile.getMediaFileType());

        Media media = mediaFile.getMedia();
        if(media != null) {
            MediaFileDto.MediaDto.MediaDtoBuilder mediaDto = MediaFileDto.MediaDto.builder();
            mediaDto.id(media.getId());
            mediaDto.mediaType(media.getMediaType());
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

        MediaView mediaView = selectedView;
        if (mediaView == null) {
            Set<MediaView> mediaViews = mediaFile.getMediaViews();
            if (mediaViews != null && !mediaViews.isEmpty()) {
                mediaView = mediaViews.iterator().next();
            }
        }
        if (mediaView != null) {
            builder.mediaViews(Set.of(toMediaViewDto(mediaView)));
        }

        // Build parent chain for episode context (episode -> season -> series)
        MediaFile parent = mediaFile.getParent();
        if (parent != null) {
            builder.parent(toParentMediaDto(parent));
        }

        return builder.build();
    }

    private static MediaFileDto.MediaViewDto toMediaViewDto(MediaView mediaView) {
        MediaFileDto.MediaViewDto.MediaViewDtoBuilder mediaViewDto = MediaFileDto.MediaViewDto.builder();
        mediaViewDto.id(mediaView.getId());
        mediaViewDto.position(mediaView.getPosition());
        mediaViewDto.duration(mediaView.getDuration());
        mediaViewDto.created(mediaView.getCreated());
        mediaViewDto.updated(mediaView.getUpdated());

        MediaUser mediaUser = mediaView.getMediaUser();
        if (mediaUser != null) {
            MediaFileDto.MediaUserDto.MediaUserDtoBuilder mediaUserDto = MediaFileDto.MediaUserDto.builder();
            mediaUserDto.id(mediaUser.getId());
            mediaUserDto.created(mediaUser.getCreated());
            mediaUserDto.updated(mediaUser.getUpdated());
            mediaUserDto.userId(mediaUser.getUserId());
            mediaViewDto.mediaUser(mediaUserDto.build());
        }

        return mediaViewDto.build();
    }

    private static MediaFileDto.ParentMediaDto toParentMediaDto(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }

        Media media = mediaFile.getMedia();
        MediaFileDto.ParentMediaDto.ParentMediaDtoBuilder builder = MediaFileDto.ParentMediaDto.builder()
                .mediaFileId(mediaFile.getMediaFileId())
                .mediaFileType(mediaFile.getMediaFileType());

        if (media != null) {
            builder.title(media.getTitle())
                    .number(media.getNumber());
        }

        // Recursively build parent chain (season -> series)
        if (mediaFile.getParent() != null) {
            builder.parent(toParentMediaDto(mediaFile.getParent()));
        }

        return builder.build();
    }
}
