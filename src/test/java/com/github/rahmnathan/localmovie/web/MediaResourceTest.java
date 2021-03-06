package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public MediaResourceTest(MediaResource mediaResource) {
        this.mediaResource = mediaResource;
    }

    @Test
    public void getMediaTest() {
        MediaRequest mediaRequest = new MediaRequest("Movies", 1, 10, MediaClient.ANDROID, MediaOrder.TITLE);

        ResponseEntity<List> response = mediaResource.getMedia(mediaRequest, new MockHttpServletResponse());

        assertEquals(HttpStatus.OK, response.getStatusCode());
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
