package com.github.rahmnathan.localmovie.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.rahmnathan.localmovie.data.SignedUrls;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Base64;

@Slf4j
@Service
public class SecurityService {
    private static final String URL_PATTERN_STREAM = "/localmovie/v1/signed/media/%s/stream?expires=%s&sig=%s";
    private static final String URL_PATTERN_POSTER = "/localmovie/v1/signed/media/%s/poster?expires=%s&sig=%s";
    private static final String URL_PATTERN_UPDATE_POSITION = "/localmovie/v1/signed/media/%s/position?expires=%s&user=%s&sig=%s";
    private static final String URL_PATTERN_SUBTITLE = "/localmovie/v1/signed/media/%s/subtitle.vtt?expires=%s&sig=%s";

    private final ObjectMapper objectMapper;

    private final byte[] key;

    @Autowired
    public SecurityService(@Value("${service.stream.hmac.key}") String hmacKey,
                           ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        this.objectMapper = mapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        key = hmacKey.getBytes();
    }

    SecurityService(String hmacKey, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        key = hmacKey.getBytes();
    }

    public boolean authorizedRequest(String mediaFileId, long expires, String signature) {
        return authorizedRequest(mediaFileId, null, expires, signature);
    }

    public boolean authorizedRequest(String mediaFileId, String userId, long expires, String signature) {
        SignedRequest signedRequest = SignedRequest.builder()
                .mediaFileId(mediaFileId)
                .userId(userId)
                .expires(expires)
                .build();

        try {
            String generatedSignature = generateSignature(signedRequest);
            return generatedSignature.equals(signature) && ZonedDateTime.now().toEpochSecond() < expires;
        } catch (JsonProcessingException e) {
            log.error("Failed generating hmac signature.", e);
            return false;
        }
    }

    public SignedUrls generateSignedPosterUrl(String mediaFileId) {
        SignedRequest signedRequest = SignedRequest.builder()
                .mediaFileId(mediaFileId)
                .expires(ZonedDateTime.now().plusDays(1L).toEpochSecond())
                .build();

        try {
            signedRequest.setSignature(generateSignature(signedRequest));
        } catch (JsonProcessingException e) {
            log.error("Failure generating signed poster urls.", e);
        }

        return SignedUrls.builder()
                .poster(formatUrl(URL_PATTERN_POSTER, signedRequest))
                .build();
    }

    public SignedUrls generateSignedUrls(String mediaFileId, String userId) throws JsonProcessingException {
        return generateSignedUrls(mediaFileId, userId, false, null);
    }

    public SignedUrls generateSignedUrls(String mediaFileId, String userId, boolean hasSubtitle, String streamContentType) throws JsonProcessingException {
        SignedRequest mediaSignedRequest = generateSignedRequest(mediaFileId, null);
        SignedRequest positionSignedRequest = generateSignedRequest(mediaFileId, userId);

        SignedUrls.SignedUrlsBuilder builder = SignedUrls.builder()
                .stream(formatUrl(URL_PATTERN_STREAM, mediaSignedRequest))
                .streamContentType(streamContentType)
                .poster(formatUrl(URL_PATTERN_POSTER, mediaSignedRequest))
                .updatePosition(formatUpdatePositionUrl(positionSignedRequest));

        if (hasSubtitle) {
            builder.subtitle(formatUrl(URL_PATTERN_SUBTITLE, mediaSignedRequest));
        }

        return builder.build();
    }

    private SignedRequest generateSignedRequest(String mediaFileId, String userId) throws JsonProcessingException {
        SignedRequest signedRequest = SignedRequest.builder()
                .mediaFileId(mediaFileId)
                .userId(userId)
                .expires(ZonedDateTime.now().plusDays(1L).toEpochSecond())
                .build();

        signedRequest.setSignature(generateSignature(signedRequest));

        return signedRequest;
    }

    private String formatUpdatePositionUrl(SignedRequest signedRequest) {
        return String.format(URL_PATTERN_UPDATE_POSITION,
                signedRequest.getMediaFileId(),
                signedRequest.getExpires(),
                signedRequest.getUserId(),
                signedRequest.getSignature());
    }

    private String generateSignature(SignedRequest signedRequest) throws JsonProcessingException {
        HMac hMac = new HMac(new SHA256Digest());
        hMac.init(new KeyParameter(key));

        byte[] hmacIn = objectMapper.writeValueAsBytes(signedRequest);
        hMac.update(hmacIn, 0, hmacIn.length);
        byte[] hmacOut = new byte[hMac.getMacSize()];

        hMac.doFinal(hmacOut, 0);
        return Base64.getUrlEncoder().encodeToString(hmacOut);
    }

    private String formatUrl(String urlPattern, SignedRequest signedRequest) {
        return String.format(urlPattern, signedRequest.getMediaFileId(), signedRequest.getExpires(), signedRequest.getSignature());
    }

    @Data
    @Builder
    private static class SignedRequest {
        private final String mediaFileId;
        private final String userId;
        private final long expires;
        @JsonIgnore
        private String signature;
    }
}
