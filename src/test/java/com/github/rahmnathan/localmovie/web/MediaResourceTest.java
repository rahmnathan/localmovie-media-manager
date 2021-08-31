package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.control.StartupMediaInitializer;
import com.github.rahmnathan.localmovie.web.webapp.MediaResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
    public void getMediaCountTest() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        mediaResource.getMediaCount("Movies", response);

        assertTrue(response.getHeaderNames().contains("Count"));
    }
}
