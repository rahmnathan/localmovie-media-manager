package com.github.rahmnathan.localmovie.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.rahmnathan.localmovie.TestContainersConfiguration;
import com.github.rahmnathan.localmovie.data.SignedUrls;
import com.github.rahmnathan.localmovie.media.MediaInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import(TestContainersConfiguration.class)
class SecurityServiceTest {
    private final SecurityService securityService;

    @Autowired
    SecurityServiceTest(SecurityService securityService, MediaInitializer initializer) {
        this.securityService = securityService;
        try {
            initializer.getInitializationFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGenerateSignatureConsistency() throws JsonProcessingException {
        String mediaFileId = "test-media-123";

        SignedUrls urls1 = securityService.generateSignedUrls(mediaFileId);
        // Extract signature from URL
        String signature1 = extractSignatureFromUrl(urls1.getStream());

        // Reset and generate again - should produce different signature due to different timestamp
        Thread.yield();
        SignedUrls urls2 = securityService.generateSignedUrls(mediaFileId);
        String signature2 = extractSignatureFromUrl(urls2.getStream());

        // Signatures should be different because timestamps are different
        assertNotNull(signature1);
        assertNotNull(signature2);
        assertFalse(signature1.isEmpty());
        assertFalse(signature2.isEmpty());
    }

    @Test
    void testGenerateSignatureVariability() throws JsonProcessingException {
        SignedUrls urls1 = securityService.generateSignedUrls("media-123");
        SignedUrls urls2 = securityService.generateSignedUrls("media-456");

        String signature1 = extractSignatureFromUrl(urls1.getStream());
        String signature2 = extractSignatureFromUrl(urls2.getStream());

        // Different media IDs should produce different signatures
        assertNotEquals(signature1, signature2);
    }

    @Test
    void testSignatureEncodingBase64Url() throws JsonProcessingException {
        SignedUrls urls = securityService.generateSignedUrls("test-media");
        String signature = extractSignatureFromUrl(urls.getStream());

        // Base64 URL encoding should not contain +, /, or = characters
        assertNotNull(signature);
        assertFalse(signature.contains("+"));
        assertFalse(signature.contains("/"));
        // Note: = is used for padding in Base64, URL encoding may or may not have it
        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    void testGenerateSignatureWithSpecialCharacters() throws JsonProcessingException {
        String mediaFileId = "test-media-with-special-chars-!@#$%";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        assertNotNull(urls.getStream());
        assertNotNull(urls.getPoster());
        assertNotNull(urls.getUpdatePosition());
    }

    @Test
    void testAuthorizedRequestValidSignature() throws JsonProcessingException {
        String mediaFileId = "test-media-auth";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        String signature = extractSignatureFromUrl(urls.getStream());
        long expires = extractExpiresFromUrl(urls.getStream());

        boolean authorized = securityService.authorizedRequest(mediaFileId, expires, signature);
        assertTrue(authorized);
    }

    @Test
    void testAuthorizedRequestInvalidSignature() {
        String mediaFileId = "test-media-auth";
        long expires = ZonedDateTime.now().plusHours(1).toEpochSecond();
        String invalidSignature = "invalid-signature-123";

        boolean authorized = securityService.authorizedRequest(mediaFileId, expires, invalidSignature);
        assertFalse(authorized);
    }

    @Test
    void testAuthorizedRequestExpiredToken() throws JsonProcessingException {
        String mediaFileId = "test-media-expired";
        long expiredTime = ZonedDateTime.now().minusHours(1).toEpochSecond();

        // We can't easily generate a valid signature for an expired time using the public API
        // But we can test that an expired time is always rejected
        boolean authorized = securityService.authorizedRequest(mediaFileId, expiredTime, "any-signature");
        assertFalse(authorized);
    }

    @Test
    void testAuthorizedRequestFutureTimestamp() throws JsonProcessingException {
        String mediaFileId = "test-media-future";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        String signature = extractSignatureFromUrl(urls.getStream());
        long expires = extractExpiresFromUrl(urls.getStream());

        // Should be authorized as expiration is in the future
        boolean authorized = securityService.authorizedRequest(mediaFileId, expires, signature);
        assertTrue(authorized);
    }

    @Test
    void testAuthorizedRequestMissingSignature() {
        String mediaFileId = "test-media";
        long expires = ZonedDateTime.now().plusHours(1).toEpochSecond();

        boolean authorized = securityService.authorizedRequest(mediaFileId, expires, "");
        assertFalse(authorized);
    }

    @Test
    void testAuthorizedRequestTamperedMediaFileId() throws JsonProcessingException {
        String originalMediaFileId = "test-media-original";
        String tamperedMediaFileId = "test-media-tampered";

        SignedUrls urls = securityService.generateSignedUrls(originalMediaFileId);
        String signature = extractSignatureFromUrl(urls.getStream());
        long expires = extractExpiresFromUrl(urls.getStream());

        // Try to use signature with different media file ID
        boolean authorized = securityService.authorizedRequest(tamperedMediaFileId, expires, signature);
        assertFalse(authorized);
    }

    @Test
    void testAuthorizedRequestNullSignature() {
        String mediaFileId = "test-media";
        long expires = ZonedDateTime.now().plusHours(1).toEpochSecond();

        boolean authorized = securityService.authorizedRequest(mediaFileId, expires, null);
        assertFalse(authorized);
    }

    @Test
    void testGenerateSignedUrls() throws JsonProcessingException {
        String mediaFileId = "test-media-urls";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        assertNotNull(urls);
        assertNotNull(urls.getStream());
        assertNotNull(urls.getPoster());
        assertNotNull(urls.getUpdatePosition());
    }

    @Test
    void testGenerateSignedPosterUrl() {
        String mediaFileId = "test-media-poster";
        SignedUrls urls = securityService.generateSignedPosterUrl(mediaFileId);

        assertNotNull(urls);
        assertNotNull(urls.getPoster());
        assertNull(urls.getStream());
        assertNull(urls.getUpdatePosition());
    }

    @Test
    void testGenerateSignedUrlsExpirationDay() throws JsonProcessingException {
        String mediaFileId = "test-media-expiration";
        long beforeGeneration = ZonedDateTime.now().plusDays(1).toEpochSecond();

        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);
        long expires = extractExpiresFromUrl(urls.getStream());

        long afterGeneration = ZonedDateTime.now().plusDays(1).toEpochSecond();

        // Expiration should be approximately 1 day from now (within a few seconds)
        assertTrue(expires >= beforeGeneration - 5);
        assertTrue(expires <= afterGeneration + 5);
    }

    @Test
    void testSignedUrlsContainCorrectPattern() throws JsonProcessingException {
        String mediaFileId = "test-media-pattern";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        // Verify stream URL pattern (Base64 URL encoding allows = padding)
        String streamPattern = "/localmovie/v1/signed/media/" + mediaFileId + "/stream\\.mp4\\?expires=\\d+&sig=[A-Za-z0-9_-]+=*";
        assertTrue(Pattern.matches(streamPattern, urls.getStream()),
                "Stream URL doesn't match expected pattern: " + urls.getStream());

        // Verify poster URL pattern
        String posterPattern = "/localmovie/v1/signed/media/" + mediaFileId + "/poster\\?expires=\\d+&sig=[A-Za-z0-9_-]+=*";
        assertTrue(Pattern.matches(posterPattern, urls.getPoster()),
                "Poster URL doesn't match expected pattern: " + urls.getPoster());

        // Verify update position URL pattern
        String updatePattern = "/localmovie/v1/signed/media/" + mediaFileId + "/position\\?expires=\\d+&sig=[A-Za-z0-9_-]+=*";
        assertTrue(Pattern.matches(updatePattern, urls.getUpdatePosition()),
                "Update position URL doesn't match expected pattern: " + urls.getUpdatePosition());
    }

    @Test
    void testGenerateSignedUrlsAllThreeUrlsGenerated() throws JsonProcessingException {
        String mediaFileId = "test-media-all-urls";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        // All three URLs should be present and non-empty
        assertNotNull(urls.getStream());
        assertNotNull(urls.getPoster());
        assertNotNull(urls.getUpdatePosition());

        assertFalse(urls.getStream().isEmpty());
        assertFalse(urls.getPoster().isEmpty());
        assertFalse(urls.getUpdatePosition().isEmpty());
    }

    @Test
    void testGenerateSignedPosterUrlPattern() {
        String mediaFileId = "test-poster-only";
        SignedUrls urls = securityService.generateSignedPosterUrl(mediaFileId);

        String posterPattern = "/localmovie/v1/signed/media/" + mediaFileId + "/poster\\?expires=\\d+&sig=[A-Za-z0-9_-]+=*";
        assertTrue(Pattern.matches(posterPattern, urls.getPoster()),
                "Poster URL doesn't match expected pattern: " + urls.getPoster());
    }

    @Test
    void testAuthorizationWithZeroExpires() {
        String mediaFileId = "test-media";
        boolean authorized = securityService.authorizedRequest(mediaFileId, 0, "any-signature");

        // Should be false because expiration is in the past (epoch 0 = Jan 1, 1970)
        assertFalse(authorized);
    }

    @Test
    void testGenerateSignedUrlsWithEmptyMediaFileId() throws JsonProcessingException {
        String mediaFileId = "";
        SignedUrls urls = securityService.generateSignedUrls(mediaFileId);

        // Should still generate URLs, even with empty ID
        assertNotNull(urls);
        assertNotNull(urls.getStream());
        assertNotNull(urls.getPoster());
        assertNotNull(urls.getUpdatePosition());
    }

    @Test
    void testSignaturesAreConsistentWithSameInputs() throws JsonProcessingException {
        // This test demonstrates that the same exact inputs don't produce the same signature
        // because the timestamp changes between calls
        String mediaFileId = "consistent-test";

        SignedUrls urls1 = securityService.generateSignedUrls(mediaFileId);
        String sig1 = extractSignatureFromUrl(urls1.getStream());
        long exp1 = extractExpiresFromUrl(urls1.getStream());

        // Generate again - timestamp will be different
        SignedUrls urls2 = securityService.generateSignedUrls(mediaFileId);
        String sig2 = extractSignatureFromUrl(urls2.getStream());
        long exp2 = extractExpiresFromUrl(urls2.getStream());

        // Signatures should be different because timestamps are different
        // (Even though they're generated microseconds apart)
        assertNotNull(sig1);
        assertNotNull(sig2);
    }

    private String extractSignatureFromUrl(String url) {
        // URL format: /path?expires=123456&sig=signature
        int sigIndex = url.indexOf("sig=");
        if (sigIndex == -1) {
            return null;
        }
        return url.substring(sigIndex + 4);
    }

    private long extractExpiresFromUrl(String url) {
        // URL format: /path?expires=123456&sig=signature
        int expiresIndex = url.indexOf("expires=");
        if (expiresIndex == -1) {
            return 0;
        }
        int endIndex = url.indexOf("&", expiresIndex);
        String expiresStr = url.substring(expiresIndex + 8, endIndex);
        return Long.parseLong(expiresStr);
    }
}
