package org.entando.kubernetes.stubhelper;

import java.time.Duration;
import org.entando.kubernetes.config.security.MultipleIdps.OAuth2IdpConfig;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;

public class TenantConfigStubHelper {

    public static final String ISSUER_URI = "http://issuer";
    public static final String REALM = "myreaml";
    public static final String HOSTNAME = "hostname";
    public static final String DB_DIALECT = "dialect";
    public static final String DB_USERNAME = "username";
    public static final String DB_PASSWORD = "password";
    public static final String DB_URL = "url";
    public static final long JWK_CACHE_TTL = 50L;
    public static final long JWK_CACHE_REFRESH = 30;
    public static final String JWK_URI = "http://jwkUri";
    public static final String TENANT_CODE = "tenantCode";

    public static OAuth2IdpConfig stubOAuth2IdpConfig(String suffix) {
        return new OAuth2IdpConfig(
                ISSUER_URI + suffix,
                Duration.ofMinutes(JWK_CACHE_TTL),
                Duration.ofMinutes(JWK_CACHE_REFRESH),
                JWK_URI + suffix,
                TENANT_CODE + suffix);
    }

    public static PrimaryTenantConfig stubPrimaryTenantConfig(String suffix) {
        return new PrimaryTenantConfig()
                .setTenantCode(TENANT_CODE + suffix)
                .setFqdns(HOSTNAME + suffix)
                .setKcAuthUrl(ISSUER_URI + suffix)
                .setDeDbDriverClassName(DB_DIALECT + suffix)
                .setDeDbUrl(DB_DIALECT + suffix)
                .setDeDbUsername(DB_DIALECT + suffix)
                .setDeDbPassword(DB_DIALECT + suffix);
    }

}
