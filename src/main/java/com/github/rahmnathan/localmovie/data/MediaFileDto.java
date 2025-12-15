package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.omdb.data.MediaType;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class MediaFileDto {
    private Long id;
    private String parentPath;
    private String path;
    private String fileName;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String mediaFileId;
    private String absolutePath;
    private Set<MediaViewDto> mediaViews;
    private SignedUrls signedUrls;
    private boolean streamable;
    private MediaFileType mediaFileType;
    private MediaDto media;

    @Data
    @Builder
    public static class MediaDto {
        private Long id;
        private MediaType mediaType;
        @ToString.Exclude
        private byte[] image;
        private String title;
        private String imdbRating;
        private String metaRating;
        private String releaseYear;
        private String actors;
        private String plot;
        private String genre;
        private Integer number;
        private LocalDateTime created;
        private LocalDateTime updated;
    }

    @Data
    @Builder
    public static class MediaViewDto {
        private Long id;
        private Double position;
        private MediaUserDto mediaUser;
        private LocalDateTime created;
        private LocalDateTime updated;
    }

    @Data
    @Builder
    public static class MediaUserDto {
        private Long id;
        private String userId;
        private LocalDateTime created;
        private LocalDateTime updated;
    }
}
