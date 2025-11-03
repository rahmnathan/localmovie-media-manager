package com.github.rahmnathan.localmovie.web.webapp;

import com.nimbusds.jose.util.Base64;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class SecurityService {
    private static final String URL_PATTERN = "/localmovie/v1/media/%s/signed/stream.mp4?expires=%s";
    private final byte[] key;

    public SecurityService(@Value("service.stream.hmac.key") String hmacKey) {
        key = hmacKey.getBytes();
    }

    public boolean authorizedRequest(String mediaFileId, long expires, String signature) {
        HMac hMac = new HMac(new SHA256Digest());
        hMac.init(new KeyParameter(key));

        byte[] hmacIn = URL_PATTERN.formatted(mediaFileId, expires).getBytes();
        hMac.update(hmacIn, 0, hmacIn.length);
        byte[] hmacOut = new byte[hMac.getMacSize()];

        hMac.doFinal(hmacOut, 0);
        String generatedSignature = Base64.encode(hmacOut).toString();
        return generatedSignature.equals(signature) && ZonedDateTime.now().toEpochSecond() < expires;
    }

    private String generateSignature(String url) {
        HMac hMac = new HMac(new SHA256Digest());
        hMac.init(new KeyParameter(key));

        byte[] hmacIn = url.getBytes();
        hMac.update(hmacIn, 0, hmacIn.length);
        byte[] hmacOut = new byte[hMac.getMacSize()];

        hMac.doFinal(hmacOut, 0);
        return Base64.encode(hmacOut).toString();
    }

    public String generateSignedUrl(String mediaFileId) {
        long expires = ZonedDateTime.now().plusDays(1L).toEpochSecond();
        String url = String.format(URL_PATTERN, mediaFileId, expires);
        return url + "&sig=" + generateSignature(url);
    }
}
