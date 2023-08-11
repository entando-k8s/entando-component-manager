package org.entando.kubernetes.config.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;

@Slf4j
@Getter
public class MultipleIdps {

    private final Map<String, OAuth2IdpConfig> trustedIssuers;

    private static final String JWK_REALM_SECTION = "/realms/";

    public MultipleIdps(List<TenantConfigDTO> tenantConfigs) {

        this.trustedIssuers = Optional.ofNullable(tenantConfigs)
                .orElseGet(ArrayList::new)
                .stream()
                .collect(Collectors.toMap(this::composeIssuerUri, tc -> {
                    String issuer = composeIssuerUri(tc);
                    return new OAuth2IdpConfig(issuer, tc.getTenantCode());
                }));

        log.debug("Extracted issuers {}", trustedIssuers.keySet());
    }

    private String composeIssuerUri(TenantConfigDTO tenantConfig) {
        if (tenantConfig instanceof PrimaryTenantConfig) {
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
        private final String tenantCode;
    }

}