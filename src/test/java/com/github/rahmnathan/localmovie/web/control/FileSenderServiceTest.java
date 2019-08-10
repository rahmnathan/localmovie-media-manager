package com.github.rahmnathan.localmovie.web.control;

import com.github.rahmnathan.localmovie.web.FileSenderService;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSenderServiceTest {
//    private final FileSenderService fileSenderService = new FileSenderService();
//
//    @Test
//    public void serveResourceTest(){
//        Path path = Paths.get("src/test/resources/vault.crt");
//
//        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
//        servletRequest.addHeader("Range", "bytes 5-1000");
//
//        HttpServletResponse servletResponse = new MockHttpServletResponse();
//
//
//        fileSenderService.serveResource(path, servletRequest, servletResponse);
//
//        System.out.println("test");
//        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, servletResponse.getStatus());
//        assertEquals("1998", servletResponse.getHeader(HttpHeaders.CONTENT_LENGTH));
//        assertEquals("bytes 5-2002/2003", servletResponse.getHeader(HttpHeaders.CONTENT_RANGE));
//    }
}
