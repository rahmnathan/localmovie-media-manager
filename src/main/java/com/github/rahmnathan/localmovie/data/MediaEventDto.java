package com.github.rahmnathan.localmovie.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaEventDto {
    private Long id;
    private LocalDateTime timestamp;
    private String relativePath;
    private String event;
    private MediaFileDto mediaFile;
}
