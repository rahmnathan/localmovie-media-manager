package com.github.rahmnathan.localmovie.web.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.rahmnathan.localmovie.web.MediaResource;
import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.web.data.MediaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MediaResourceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String PATH = "Series";
    private MediaResource mediaResource;

//    @MockBean
//    private MediaDataServiceWeb dataService;
//
//    @Autowired
//    public MediaResourceTest(MediaResource mediaResource){
//        this.mediaResource = mediaResource;
//    }
//
//    @Test
//    public void getMoviesTest(){
//        MediaRequest mediaRequest = new MediaRequest(PATH, 0, 10, MediaClient.ANDROID, null);
//        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
//        when(dataService.loadMediaListLength(PATH)).thenReturn(1);
//        when(dataService.loadMediaFileList(mediaRequest)).thenReturn(List.of(MAPPER.createObjectNode().put("path", PATH)));
//
//        List<JsonNode> mediaFiles = mediaResource.getMedia(mediaRequest, servletResponse, mock(Principal.class));
//
//        assertEquals(1, mediaFiles.size());
//        assertEquals(PATH, mediaFiles.get(0).get("path").textValue());
//    }
//
//    @Test
//    public void getPosterTest(){
//        String poster = "My fake poster";
//        JsonNode media = MAPPER.createObjectNode().put("image", Base64.getEncoder().encodeToString(poster.getBytes()));
//        JsonNode mediaFile = MAPPER.createObjectNode().set("media", media);
//
//        when(dataService.loadSingleMediaFile(PATH)).thenReturn(mediaFile);
//
//        byte[] resultPoster = mediaResource.getPoster(PATH);
//
//        assertArrayEquals(poster.getBytes(), resultPoster);
//    }
//
//    @Test
//    public void getMediaEventsTest(){
//        long epoch = 123456789;
//        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
//        JsonNode mediaFileEvent = MAPPER.createObjectNode().put("path", PATH);
//        when(dataService.getMediaFileEvents(localDateTime)).thenReturn(List.of(mediaFileEvent));
//
//        List<JsonNode> mediaFileEvents = mediaResource.getEvents(epoch);
//
//        assertEquals(1, mediaFileEvents.size());
//        assertEquals(PATH, mediaFileEvents.get(0).get("path").textValue());
//    }
}
