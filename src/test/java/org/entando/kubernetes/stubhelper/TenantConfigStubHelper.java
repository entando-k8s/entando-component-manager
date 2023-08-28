package org.entando.kubernetes.stubhelper;

import org.entando.kubernetes.config.security.MultipleIdps.OAuth2IdpConfig;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;

public class TenantConfigStubHelper {

    public static final String ISSUER_URI = "http://issuer";
    public static final String REALM = "myreaml";
    public static final String HOSTNAME = "hostname";
    public static final String DB_DIALECT = "dialect";
    public static final String TENANT_CODE = "tenantCode";

    public static OAuth2IdpConfig stubOAuth2IdpConfig(String suffix) {
        return new OAuth2IdpConfig(
                ISSUER_URI + suffix,
                TENANT_CODE + suffix);
    }

    public static PrimaryTenantConfig stubPrimaryTenantConfig(String suffix) {
        return new PrimaryTenantConfig()
                .setTenantCode(TENANT_CODE + suffix)
                .setFqdns(HOSTNAME + suffix)
                .setKcAuthUrl(ISSUER_URI + suffix)
                .setCmDbJdbcUrl(DB_DIALECT + suffix)
                .setCmDbUsername(DB_DIALECT + suffix)
                .setCmDbPassword(DB_DIALECT + suffix);
    }

}
