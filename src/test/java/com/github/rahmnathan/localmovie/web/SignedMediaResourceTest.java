package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.repository.MediaSubtitleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignedMediaResourceTest {

    @Mock
    private MediaPersistenceService persistenceService;
    @Mock
    private MediaStreamingService mediaStreamingService;
    @Mock
    private SecurityService securityService;
    @Mock
    private MediaSubtitleRepository subtitleRepository;

    @Test
    void getPosterAllowsUnsignedPosterForClientCompatibility() {
        SignedMediaResource resource = resource();
        byte[] poster = new byte[]{1, 2, 3};
        when(persistenceService.getMediaImageById("media-id")).thenReturn(poster);

        var response = resource.getPoster("media-id", null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(poster, response.getBody());
        verifyNoInteractions(securityService);
    }

    @Test
    void getPosterRejectsInvalidSignatureWhenSignatureParametersArePresent() {
        SignedMediaResource resource = resource();
        when(securityService.authorizedRequest("media-id", 123L, "bad-signature")).thenReturn(false);

        var response = resource.getPoster("media-id", 123L, "bad-signature");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void getPosterReturnsImageForValidSignature() {
        SignedMediaResource resource = resource();
        byte[] poster = new byte[]{1, 2, 3};
        when(securityService.authorizedRequest("media-id", 123L, "valid-signature")).thenReturn(true);
        when(persistenceService.getMediaImageById("media-id")).thenReturn(poster);

        var response = resource.getPoster("media-id", 123L, "valid-signature");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(poster, response.getBody());
    }

    private SignedMediaResource resource() {
        return new SignedMediaResource(
                persistenceService,
                mediaStreamingService,
                securityService,
                subtitleRepository);
    }
}
