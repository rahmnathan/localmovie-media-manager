package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.control.StartupMediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileNoPoster;
import com.github.rahmnathan.localmovie.web.webapp.MediaResource;
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
public class MediaResourceTest {
    private final MediaResource mediaResource;

    @Autowired
    public MediaResourceTest(MediaResource mediaResource, StartupMediaInitializer initializer) {
        this.mediaResource = mediaResource;
        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getMediaTest() {
        List<MediaFile> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());
        assertTrue(mediaFileList.size() > 0);
    }

    @Test
    public void getMediaCountTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.getMediaCount("Movies", response);

        assertTrue(response.getHeaderNames().contains("Count"));
    }

    @Test
    public void streamVideoTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<MediaFile> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());
        mediaResource.streamVideo(mediaFileList.get(0).getMediaFileId(), response, new MockHttpServletRequest());

        assertTrue(response.getHeaderNames().contains("Content-Range"));
        assertTrue(response.getHeaderNames().contains("Content-Length"));
    }

    @Test
    public void getPosterTest() {
        List<MediaFile> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        byte[] poster = mediaResource.getPoster(mediaFileList.get(0).getMediaFileId());
        assertNotNull(poster);
    }

    @Test
    @Transactional
    public void addViewTest() {
        List<MediaFile> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        double position = 10.0;
        mediaResource.updatePosition(mediaFileList.get(0).getMediaFileId(), position);

        Optional<MediaFile> mediaOptional = mediaResource.getMedia(mediaFileList.get(0).getMediaFileId());

        assertTrue(mediaOptional.isPresent());
        assertEquals(position, mediaOptional.get().getMediaViews().iterator().next().getPosition());
    }

    @Test
    @Transactional
    public void addMultipleViewsTest() {
        List<MediaFile> mediaFileList = mediaResource.getMedia(buildRequest(), new MockHttpServletResponse());

        double position = 10.0;
        mediaResource.updatePosition(mediaFileList.get(0).getMediaFileId(), position);
        position += 10;
        mediaResource.updatePosition(mediaFileList.get(0).getMediaFileId(), position);

        Optional<MediaFile> mediaOptional = mediaResource.getMedia(mediaFileList.get(0).getMediaFileId());

        assertTrue(mediaOptional.isPresent());
        assertEquals(position, mediaOptional.get().getMediaViews().iterator().next().getPosition());
    }

    private MediaRequest buildRequest() {
        return new MediaRequest("Movies", 0, 100, null, null, null);
    }
}
