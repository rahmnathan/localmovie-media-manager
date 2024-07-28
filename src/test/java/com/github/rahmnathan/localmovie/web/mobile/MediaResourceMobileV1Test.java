package com.github.rahmnathan.localmovie.web.mobile;

import com.github.rahmnathan.localmovie.control.StartupMediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaOrder;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.github.rahmnathan.localmovie.web.mobile.MediaResourceMobileV1.RESPONSE_HEADER_COUNT;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MediaResourceMobileV1Test {
    private final MediaResourceMobileV1 mediaResource;

    @Autowired
    public MediaResourceMobileV1Test(MediaResourceMobileV1 mediaResource, StartupMediaInitializer initializer) {
        this.mediaResource = mediaResource;
        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getEventsTest() {
        List<MediaFileEvent> events = mediaResource.getEvents(System.currentTimeMillis(), Pageable.unpaged(), new MockHttpServletResponse());
        assertEquals(0, events.size());
    }

    @Test
    public void countEventsTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.countEvents(System.currentTimeMillis(), response);

        assertEquals(0, Integer.valueOf(response.getHeader(RESPONSE_HEADER_COUNT)));
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

    private MediaRequest buildRequest() {
        return new MediaRequest("Movies", 0, 100, null, null, null);
    }
}
