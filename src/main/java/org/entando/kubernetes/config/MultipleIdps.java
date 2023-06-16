/*
 * Copyright 2023-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.kubernetes.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.TenantConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Slf4j
@Configuration
@DependsOn("tenantConfiguration")
class MultipleIdps {
    
    public final Map<String, OAuth2IdpConfig> trustedIssuers;
    
    private static final String JWK_REALM_SECTION = "/realms/";
    private static final String JWK_URI_SUFFIX = "/protocol/openid-connect/certs";
    
    public MultipleIdps(@Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuerUri, 
            @Autowired Map<String, TenantConfig> tenantConfigs) {
        Duration defaultJwkCacheTtl = Duration.ofMinutes(30l);
        Duration defaultJwkCacheRefresh = Duration.ofMinutes(15l);
        String defaultJwkSetUri = issuerUri + JWK_URI_SUFFIX;
        OAuth2IdpConfig defaultIdp = new OAuth2IdpConfig(issuerUri, defaultJwkCacheTtl, defaultJwkCacheRefresh, defaultJwkSetUri);
        Map<String, OAuth2IdpConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put(issuerUri, defaultIdp);
        Map<String, OAuth2IdpConfig> tenantTustedIssuers = tenantConfigs.values().stream().collect(Collectors.toMap(tc -> tc.getKcAuthUrl()+ JWK_REALM_SECTION + tc.getKcRealm(), tc -> {
            String issuer = tc.getKcAuthUrl() + JWK_REALM_SECTION + tc.getKcRealm();
            Duration jwkCacheTtl = Duration.ofMinutes(30l);
            Duration jwkCacheRefresh = Duration.ofMinutes(15l);
            String jwkSetUri = tc.getKcAuthUrl() + JWK_REALM_SECTION + tc.getKcRealm() + JWK_URI_SUFFIX;
            return new OAuth2IdpConfig(issuer, jwkCacheTtl, jwkCacheRefresh, jwkSetUri);
        }));
        trustedIssuers.putAll(tenantTustedIssuers);
        log.debug("Extracted issuers {}", trustedIssuers.keySet());
        this.trustedIssuers = trustedIssuers;
    }

    public boolean isTrustedIssuer(String issuer) {
        return trustedIssuers.keySet().contains(issuer);
    }

    public OAuth2IdpConfig getIdpConfigForIssuer(String issuer) {
        return trustedIssuers.get(issuer);
    }
    
    @Getter@Setter
    public static class OAuth2IdpConfig {
        
        private final String issuerUri;
        private final Duration jwkCacheTtl;
        private final Duration jwkCacheRefresh;
        private final String jwkUri;

        public OAuth2IdpConfig(
                String issuerUri,
                Duration jwkCacheTtl,
                Duration jwkCacheRefresh,
                String jwkSetUri) {
            this.issuerUri = issuerUri;
            this.jwkCacheTtl = jwkCacheTtl;
            this.jwkCacheRefresh = jwkCacheRefresh;
            this.jwkUri = jwkSetUri;
        }

        @Override
        public String toString() {
            return "OAuth2Config{"
                    + "issuerUri='"
                    + issuerUri
                    + '\''
                    + ", jwkCacheTtl="
                    + jwkCacheTtl
                    + '\''
                    + ", jwkCacheRefresh="
                    + jwkCacheRefresh
                    + ", jwkUri='"
                    + jwkUri
                    + '\''
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OAuth2IdpConfig that = (OAuth2IdpConfig) o;
            return issuerUri.equals(that.issuerUri)
                    && jwkCacheTtl.equals(that.jwkCacheTtl)
                    && jwkCacheRefresh.equals(that.jwkCacheRefresh)
                    && jwkUri.equals(that.jwkUri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issuerUri, jwkCacheTtl, jwkCacheRefresh, jwkUri);
        }
        
    }
    
}
