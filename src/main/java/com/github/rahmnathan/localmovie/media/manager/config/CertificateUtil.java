package com.github.rahmnathan.localmovie.media.manager.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.*;

import java.util.Collections;

class CertificateUtil {
    private final static Logger logger = LoggerFactory.getLogger(CertificateUtil.class);

    // Refresh period in seconds before certificate expires.
    private final static long REFRESH_PERIOD_BEFORE_EXPIRY = 60;

    /**
     * Request SSL Certificate from Vault or retrieve cached certificate.
     * <p>
     * If {@link VaultPkiProperties#isReuseValidCertificate()} is enabled this method
     * attempts to read a cached Certificate from Vault at {@code secret/$
     * spring.application.name}/cert/${spring.cloud.vault.pki.commonName}}. Valid
     * certificates will be reused until they expire. A new certificate is requested and
     * cached if no valid certificate is found.
     *
     * @param vaultProperties
     * @param vaultOperations
     * @param pkiProperties
     * @return the {@link CertificateBundle}.
     */
    public static CertificateBundle getOrRequestCertificate(VaultProperties vaultProperties, VaultOperations vaultOperations,
                                                            VaultPkiProperties pkiProperties) {

        CertificateBundle validCertificate = findValidCertificate(vaultProperties, vaultOperations, pkiProperties);
        if (!pkiProperties.isReuseValidCertificate()) {
            return validCertificate;
        }

        String cacheKey = createCacheKey(vaultProperties, pkiProperties);
        vaultOperations.delete(cacheKey);

        VaultCertificateResponse certificateResponse = requestCertificate(vaultOperations, pkiProperties);
        VaultHealth health = vaultOperations.opsForSys().health();
        storeCertificate(cacheKey, vaultOperations, health, certificateResponse);

        return certificateResponse.getData();
    }

    public static CertificateBundle findValidCertificate(VaultProperties vaultProperties,
                                                         VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {
        if (!pkiProperties.isReuseValidCertificate()) {
            return requestCertificate(vaultOperations, pkiProperties).getData();
        }

        String cacheKey = createCacheKey(vaultProperties, pkiProperties);
        VaultResponseSupport<CachedCertificateBundle> readResponse = vaultOperations.read(cacheKey, CachedCertificateBundle.class);

        VaultHealth health = vaultOperations.opsForSys().health();
        if (isValid(health, readResponse)) {
            logger.info("Found valid SSL certificate in Vault for: {}", pkiProperties.getCommonName());
            return getCertificateBundle(readResponse);
        }

        return null;
    }

    private static void storeCertificate(String cacheKey, VaultOperations vaultOperations, VaultHealth health,
                                         VaultCertificateResponse certificateResponse) {

        CertificateBundle certificateBundle = certificateResponse.getData();
        long expires = (health.getServerTimeUtc() + certificateResponse.getLeaseDuration()) - REFRESH_PERIOD_BEFORE_EXPIRY;

        CachedCertificateBundle cachedCertificateBundle = new CachedCertificateBundle();
        cachedCertificateBundle.setExpires(expires);
        cachedCertificateBundle.setTimeRequested(health.getServerTimeUtc());
        cachedCertificateBundle.setPrivateKey(certificateBundle.getPrivateKey());
        cachedCertificateBundle.setCertificate(certificateBundle.getCertificate());
        cachedCertificateBundle.setIssuingCaCertificate(certificateBundle.getIssuingCaCertificate());
        cachedCertificateBundle.setSerialNumber(certificateBundle.getSerialNumber());

        vaultOperations.write(cacheKey, cachedCertificateBundle);
    }

    private static String createCacheKey(VaultProperties vaultProperties, VaultPkiProperties pkiProperties) {
        return String.format("secret/%s/cert/%s", vaultProperties.getApplicationName(), pkiProperties.getCommonName());
    }

    private static CertificateBundle getCertificateBundle(VaultResponseSupport<CachedCertificateBundle> readResponse) {
        CachedCertificateBundle cachedCertificateBundle = readResponse.getData();
        return CertificateBundle.of(cachedCertificateBundle.getSerialNumber(),
                cachedCertificateBundle.getCertificate(),
                cachedCertificateBundle.getIssuingCaCertificate(),
                cachedCertificateBundle.getPrivateKey());
    }

    private static boolean isValid(VaultHealth health, VaultResponseSupport<CachedCertificateBundle> readResponse) {
        if (readResponse != null) {
            CachedCertificateBundle cachedCertificateBundle = readResponse.getData();
            return health.getServerTimeUtc() < cachedCertificateBundle.getExpires();
        }

        return false;
    }

    private static VaultCertificateResponse requestCertificate(VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {
        logger.info("Requesting SSL certificate from Vault for: {}", pkiProperties.getCommonName());

        VaultCertificateRequest certificateRequest = VaultCertificateRequest
                .builder()
                .commonName(pkiProperties.getCommonName())
                .altNames(pkiProperties.getAltNames() != null ? pkiProperties.getAltNames() : Collections.emptyList()).build();

        return vaultOperations.opsForPki(pkiProperties.getBackend()).issueCertificate(pkiProperties.getRole(), certificateRequest);
    }

    static class CachedCertificateBundle {

        private String certificate;

        @JsonProperty("serial_number")
        private String serialNumber;

        @JsonProperty("issuing_ca")
        private String issuingCaCertificate;

        @JsonProperty("private_key")
        private String privateKey;

        @JsonProperty("time_requested")
        private long timeRequested;

        @JsonProperty("expires")
        private long expires;

        public String getCertificate() {
            return certificate;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public String getIssuingCaCertificate() {
            return issuingCaCertificate;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public long getTimeRequested() {
            return timeRequested;
        }

        public long getExpires() {
            return expires;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public void setIssuingCaCertificate(String issuingCaCertificate) {
            this.issuingCaCertificate = issuingCaCertificate;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public void setTimeRequested(long timeRequested) {
            this.timeRequested = timeRequested;
        }

        public void setExpires(long expires) {
            this.expires = expires;
        }
    }
}