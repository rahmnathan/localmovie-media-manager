package com.github.rahmnathan.localmovie.media.manager.config.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties("vault.pki")
@Validated
public class VaultPkiProperties implements VaultSecretBackendDescriptor {

    /**
     * Enable pki backend usage.
     */
    private boolean enabled = true;

    /**
     * Role name for credentials.
     */
    private String role = "localmovies";

    /**
     * pki backend path.
     */
    private String backend = "pki";

    /**
     * The CN of the certificate. Should match the host name.
     */
    private String commonName = "localmovies";

    /**
     * Alternate CN names for additional host names.
     */
    private List<String> altNames;

    /**
     * Prevent certificate re-creation by storing the Valid certificate inside Vault.
     */
    private boolean reuseValidCertificate = true;

    /**
     * Startup/Locking timeout. Used to synchronize startup and to prevent multiple SSL
     * certificate requests.
     */
    private int startupLockTimeout = 10000;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public void setAltNames(List<String> altNames) {
        this.altNames = altNames;
    }

    public void setReuseValidCertificate(boolean reuseValidCertificate) {
        this.reuseValidCertificate = reuseValidCertificate;
    }

    public void setStartupLockTimeout(int startupLockTimeout) {
        this.startupLockTimeout = startupLockTimeout;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String getBackend() {
        return backend;
    }

    public String getCommonName() {
        return commonName;
    }

    public List<String> getAltNames() {
        return altNames;
    }

    public boolean isReuseValidCertificate() {
        return reuseValidCertificate;
    }

    public int getStartupLockTimeout() {
        return startupLockTimeout;
    }
}