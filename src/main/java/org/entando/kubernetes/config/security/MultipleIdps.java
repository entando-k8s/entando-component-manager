package org.entando.kubernetes.config.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Getter
public class MultipleIdps {

    private final Map<String, OAuth2IdpConfig> trustedIssuers;

    private static final String JWK_REALM_SECTION = "/realms/";
    private static final String JWK_URI_SUFFIX = "/protocol/openid-connect/certs";
    private static final long JWK_CACHE_TTL_DURATION = 30L;
    private static final long JWK_CACHE_REFRESH_DURATION = 15L;

    public MultipleIdps(@Autowired List<TenantConfigDTO> tenantConfigs) {

        this.trustedIssuers = Optional.ofNullable(tenantConfigs)
                .orElseGet(ArrayList::new)
                .stream()
                .collect(Collectors.toMap(this::composeIssuerUri, tc -> {
                    String issuer = composeIssuerUri(tc);
                    Duration jwkCacheTtl = Duration.ofMinutes(JWK_CACHE_TTL_DURATION);
                    Duration jwkCacheRefresh = Duration.ofMinutes(JWK_CACHE_REFRESH_DURATION);
                    String jwkSetUri = issuer + JWK_URI_SUFFIX;
                    return new OAuth2IdpConfig(issuer, jwkCacheTtl, jwkCacheRefresh, jwkSetUri, tc.getTenantCode());
                }));

        log.debug("Extracted issuers {}", trustedIssuers.keySet());
    }

    private String composeIssuerUri(TenantConfigDTO tenantConfig) {
        if (StringUtils.isBlank(tenantConfig.getKcRealm())) {
            return tenantConfig.getKcAuthUrl();
        } else {
            return tenantConfig.getKcAuthUrl() + JWK_REALM_SECTION + tenantConfig.getKcRealm();
        }
    }

    public boolean isTrustedIssuer(String issuer) {
        return trustedIssuers.containsKey(issuer);
    }

    public OAuth2IdpConfig getIdpConfigForIssuer(String issuer) {
        return trustedIssuers.get(issuer);
    }

    @Data
    @AllArgsConstructor
    public static class OAuth2IdpConfig {

        private final String issuerUri;
        private final Duration jwkCacheTtl;
        private final Duration jwkCacheRefresh;
        private final String jwkUri;
        private final String tenantCode;
    }

}