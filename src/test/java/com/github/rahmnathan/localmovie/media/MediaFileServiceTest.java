package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.TestContainersConfiguration;
import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import(TestContainersConfiguration.class)
class MediaFileServiceTest {
    private final MediaFileService mediaFileService;

    @Autowired
    MediaFileServiceTest(MediaFileService mediaFileService, MediaInitializer initializer) {
        this.mediaFileService = mediaFileService;

        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> filePaths() {
        return Stream.of(
                Arguments.of("/home/test/LocalMedia/Movies/300.mkv"),
                Arguments.of("/home/test/LocalMedia/Series/Game of Thrones/Season 1"),
                Arguments.of("/home/test/LocalMedia/Series/Game of Thrones/Season 1/Episode 1.mkv")
        );
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    void loadMediaFileTest(String path) throws InvalidMediaException {
        MediaFile mediaFile = mediaFileService.loadMediaFile(MediaPath.parse(path));
        assertNotNull(mediaFile);
    }
}
