package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

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
    void getMediaTest() throws Exception {
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());
        assertFalse(mediaFileList.isEmpty());
    }

    @Test
    void getMediaCountTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.getMediaCount(buildRequest(), response);

        assertTrue(response.getHeaderNames().contains("Count"));
    }

    @Test
    void getPosterTest() throws Exception {
        List<MediaFileDto> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        byte[] poster = mediaResource.getPoster(mediaFileList.get(0).getMediaFileId());
        assertNotNull(poster);
    }

    private MediaRequest buildRequest() {
        return new MediaRequest("Movies", "", 0, 100, null, null, null, "movies");
    }
}
