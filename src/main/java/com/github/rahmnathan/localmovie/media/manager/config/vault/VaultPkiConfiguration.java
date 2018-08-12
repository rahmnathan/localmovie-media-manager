package com.github.rahmnathan.localmovie.media.manager.config.vault;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.CertificateBundle;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "spring.cloud.vault", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VaultPkiProperties.class)
public class VaultPkiConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.vault", name = "enabled", havingValue = "true")
    public static SslCertificateEmbeddedServletContainerCustomizer sslCertificateRequestingPostProcessor(
            VaultProperties vaultProperties, VaultOperations vaultOperations,
            VaultPkiProperties pkiProperties, ServerProperties serverProperties) {

        CertificateBundle certificateBundle = CertificateUtil.findValidCertificate(vaultProperties, vaultOperations, pkiProperties);

        if (certificateBundle != null) {
            return createCustomizer(serverProperties, certificateBundle);
        }

        certificateBundle = CertificateUtil.getOrRequestCertificate(vaultProperties, vaultOperations, pkiProperties);
        return createCustomizer(serverProperties, certificateBundle);
    }

    private static SslCertificateEmbeddedServletContainerCustomizer createCustomizer(
            ServerProperties serverProperties, CertificateBundle certificateBundle) {
        Ssl ssl = serverProperties.getSsl();

        if (ssl != null) {
            ssl.setKeyAlias("vault");
            ssl.setKeyPassword("");
            ssl.setKeyStorePassword("");
        }

        return new SslCertificateEmbeddedServletContainerCustomizer(certificateBundle);
    }

    private static class SslCertificateEmbeddedServletContainerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

        private final CertificateBundle certificateBundle;

        SslCertificateEmbeddedServletContainerCustomizer(CertificateBundle certificateBundle) {
            this.certificateBundle = certificateBundle;
        }

        @Override
        public void customize(TomcatServletWebServerFactory container) {
            try {
                final KeyStore keyStore = certificateBundle.createKeyStore("vault");
                final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

                trustStore.load(null, null);
                trustStore.setCertificateEntry("ca", certificateBundle.getX509IssuerCertificate());
                trustStore.setCertificateEntry("cert", certificateBundle.getX509Certificate());

                container.setSslStoreProvider(new SslStoreProvider() {
                    @Override
                    public KeyStore getKeyStore() {
                        return keyStore;
                    }

                    @Override
                    public KeyStore getTrustStore() {
                        return trustStore;
                    }
                });
            } catch (IOException | GeneralSecurityException e) {
                throw new IllegalStateException("Cannot configure Vault SSL certificate in ConfigurableEmbeddedServletContainer", e);
            }
        }
    }
}