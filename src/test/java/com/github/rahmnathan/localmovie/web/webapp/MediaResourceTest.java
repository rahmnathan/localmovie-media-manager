package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MediaResourceTest {
    private final MediaResource mediaResource;

    @Autowired
    MediaResourceTest(MediaResource mediaResource, MediaInitializer initializer) {
        this.mediaResource = mediaResource;
        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getMediaTest() {
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());
        assertTrue(mediaFileList.size() > 0);
    }

    @Test
    void getMediaCountTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.getMediaCount(buildRequest(), response);

        assertTrue(response.getHeaderNames().contains("Count"));
    }

    @Test
    void streamVideoTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());
        mediaResource.streamVideo(mediaFileList.get(0).getMediaFileId(), response, new MockHttpServletRequest());

        assertTrue(response.getHeaderNames().contains("Content-Range"));
        assertTrue(response.getHeaderNames().contains("Content-Length"));
    }

    @Test
    void getPosterTest() {
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        byte[] poster = mediaResource.getPoster(mediaFileList.get(0).getMediaFileId());
        assertNotNull(poster);
    }

    @Test
    @Transactional
    void addViewTest() {
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        double position = 10.0;
        mediaResource.updatePosition(mediaFileList.get(0).getMediaFileId(), position);

        Optional<MediaFileDto> mediaOptional = mediaResource.getMedia(mediaFileList.get(0).getMediaFileId());

        assertTrue(mediaOptional.isPresent());
        assertEquals(position, mediaOptional.get().getMediaViews().iterator().next().getPosition());
    }

    @Test
    @Transactional
    void addMultipleViewsTest() {
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        double position = 10.0;
        mediaResource.updatePosition(mediaFileList.get(0).getMediaFileId(), position);
        position += 10;
        mediaResource.updatePosition(mediaFileList.get(0).getMediaFileId(), position);

        Optional<MediaFileDto> mediaOptional = mediaResource.getMedia(mediaFileList.get(0).getMediaFileId());

        assertTrue(mediaOptional.isPresent());
        assertEquals(position, mediaOptional.get().getMediaViews().iterator().next().getPosition());
    }

    private MediaRequest buildRequest() {
        return new MediaRequest("Movies", 0, 100, null, null, null, "movies");
    }
}
