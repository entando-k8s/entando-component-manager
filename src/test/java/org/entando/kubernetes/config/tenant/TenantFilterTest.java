package org.entando.kubernetes.config.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TenantFilterTest {

    private TenantFilter filter;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void init() {
        StringBuilder config = new StringBuilder();
        config.append("[");
        config.append(getTenantConfigMock("tenant1", "test.entando.com, test2.entando.com, tenant1.entando.com"));
        config.append(",");
        config.append(getTenantConfigMock("tenant2", "tenant2.entando.com, test3.entando.com"));
        config.append(",");
        config.append(getTenantConfigMock("tenant3", "tenant3.entando.com, test4.entando.com"));
        config.append("]");
        String tenantsConfig = config.toString();
        TenantConfiguration tenantConfiguration = new TenantConfiguration(tenantsConfig, objectMapper);
        filter = new TenantFilter(tenantConfiguration);
    }

    @Test
    void getByExistingXForwardedHostHeaderShouldReturnTheTenantCode() {
        String tenantCode = filter.getTenantCode("tenant2.entando.com", "", "");
        assertEquals("tenant2", tenantCode);
    }

    @Test
    void getByNotExistingXForwardedHostHeaderShouldReturnPrimary() {
        String tenantCode = filter.getTenantCode("tenant256.entando.com", "", "");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingHostHeaderShouldReturnTheTenantCode() {
        String tenantCode = filter.getTenantCode("", "tenant2.entando.com", "");
        assertEquals("tenant2", tenantCode);
    }

    @Test
    void getByNotExistingHostHeaderShouldReturnPrimary() {
        String tenantCode = filter.getTenantCode("", "tenant256.entando.com", "");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByNotExistingXForwardedHostHeaderAndExistingHostShouldReturnPrimary() {
        String tenantCode = filter.getTenantCode("tenant256.entando.com", "tenant2.entando.com", "");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingServletNameShouldReturnTheTenantCode() {
        String tenantCode = filter.getTenantCode("", "", "tenant1.entando.com");
        assertEquals("tenant1", tenantCode);
    }

    @Test
    void getByNotExistingServletNameShouldReturnPrimary() {
        String tenantCode = filter.getTenantCode("", "", "tenant256.entando.com");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getPrimaryIfConfigNull() {
        TenantFilter filter2 = new TenantFilter(null);
        String tenantCode = filter2.getTenantCode("tenant2.entando.com", "", "");
        assertEquals("primary", tenantCode);
    }

    private String getTenantConfigMock(String tenantName, String fqdns) {
        return "{"
                + "\"dbMaxTotal\":\"1\","
                + "\"tenantCode\":\"" + tenantName + "\","
                + "\"initializationAtStartRequired\":\"false\","
                + "\"fqdns\":\" " + fqdns + "\" ,"
                + "\"kcEnabled\":true,"
                + "\"kcAuthUrl\":\"mock-auth-url\","
                + "\"kcRealm\":\"tenant1\","
                + "\"kcClientId\":\"mock-client-id\","
                + "\"kcClientSecret\":\"mock-client-secret\","
                + "\"kcPublicClientId\":\"mock\","
                + "\"kcSecureUris\":\"\","
                + "\"kcDefaultAuthorizations\":\"\","
                + "\"dbDriverClassName\":\"org.postgresql.Driver\","
                + "\"dbUrl\":\"jdbc:postgresql\","
                + "\"dbUsername\":\"username\","
                + "\"dbPassword\":\"password\","
                + "\"cdsPublicUrl\":\"mock\","
                + "\"cdsPrivateUrl\":\"mock\","
                + "\"cdsPath\":\"api/v1\","
                + "\"solrAddress\":\"mock\","
                + "\"solrCore\":\"mock\"}";
    }

}
