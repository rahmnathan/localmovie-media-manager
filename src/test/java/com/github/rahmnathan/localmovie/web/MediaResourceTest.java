package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.control.StartupMediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.data.MediaFile;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

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
        MediaRequest mediaRequest = new MediaRequest("Movies", 0, 10, MediaClient.ANDROID, MediaOrder.TITLE);

        List<? extends MediaFile> response = mediaResource.getMedia(mediaRequest, new MockHttpServletResponse());

        assertTrue(response.size() > 0);
    }

    @Test
    public void getMediaCountTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.getMediaCount("Movies", response);

        assertTrue(response.getHeaderNames().contains("Count"));
    }

    @Test
    public void getEventsTest() {
        List<MediaFileEvent> events = mediaResource.getEvents(System.currentTimeMillis(), Pageable.unpaged(), new MockHttpServletResponse());

        assertEquals(0, events.size());
    }

    @Test
    public void streamVideoTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.streamVideo("Movies/300.mkv", response, new MockHttpServletRequest());

        assertTrue(response.getHeaderNames().contains("Content-Range"));
        assertTrue(response.getHeaderNames().contains("Content-Length"));
    }

    @Test
    public void getPosterTest() {
        byte[] poster = mediaResource.getPoster("Movies/300.mkv");
        assertNotNull(poster);
    }
}
