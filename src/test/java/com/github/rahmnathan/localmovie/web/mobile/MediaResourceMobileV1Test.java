package com.github.rahmnathan.localmovie.web.mobile;

import com.github.rahmnathan.localmovie.media.MediaInitializer;
import com.github.rahmnathan.localmovie.data.MediaEventDto;
import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Objects;

import static com.github.rahmnathan.localmovie.web.mobile.MediaResourceMobileV1.RESPONSE_HEADER_COUNT;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MediaResourceMobileV1Test {
    private final MediaResourceMobileV1 mediaResource;

    @Autowired
    MediaResourceMobileV1Test(MediaResourceMobileV1 mediaResource, MediaInitializer initializer) {
        this.mediaResource = mediaResource;
        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getEventsTest() {
        List<MediaEventDto> events = mediaResource.getEvents(System.currentTimeMillis(), Pageable.unpaged(), new MockHttpServletResponse());
        assertEquals(0, events.size());
    }

    @Test
    void countEventsTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.countEvents(System.currentTimeMillis(), response);

        assertEquals(0, Integer.valueOf(Objects.requireNonNull(response.getHeader(RESPONSE_HEADER_COUNT))));
    }

    @Test
    void getMediaTest() {
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
    void getHistoryTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        mediaResource.getMediaHistory(buildRequest(), response);

        assertTrue(response.getHeaderNames().contains("Count"));
    }

    private MediaRequest buildRequest() {
        return new MediaRequest("Movies", 0, 100, null, null, null, "movies");
    }
}
