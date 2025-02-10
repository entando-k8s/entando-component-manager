package org.entando.kubernetes.config.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TenantFilterUtilsTest {

    List<TenantConfigDTO> configDTOList;

    @BeforeEach
    void init() throws JsonProcessingException {
        StringBuilder config = new StringBuilder();
        config.append("[");
        config.append(getTenantConfigMock("tenant1", "test.entando.com, test2.entando.com, tenant1.entando.com", "context1"));
        config.append(",");
        config.append(getTenantConfigMock("tenant2", "tenant2.entando.com, test3.entando.com", "context2"));
        config.append(",");
        config.append(getTenantConfigMock("tenant3", "tenant2.entando.com, test3.entando.com", ""));
        config.append(",");
        config.append(getTenantConfigMock("tenant4", "tenant3.entando.com, test4.entando.com", null));
        config.append("]");
        String tenantsConfig = config.toString();
        configDTOList = (new ObjectMapper()).readValue(tenantsConfig,
                new TypeReference<List<TenantConfigDTO>>() {
                });
    }

    @Test
    void shouldReturnTheTenantCodeFromExistingXEntandoCustomHeader() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "xtenant", "tenant2.entando.com", "", "", "xcontext");
        assertEquals("xtenant", tenantCode);
    }

    @Test
    void getByExistingXForwardedHostHeaderAndContextShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant2.entando.com", "", "", "context2");
        assertEquals("tenant2", tenantCode);
    }

    @Test
    void getByNotExistingXForwardedHostHeaderOrContextShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant256.entando.com", "", "","context2");
        assertEquals("primary", tenantCode);
        tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant2.entando.com", "", "","context256");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingHostHeaderAndContextShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "tenant2.entando.com", "", "context2");
        assertEquals("tenant2", tenantCode);
    }

    @Test
    void getByNotExistingHostHeaderOrContextShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "tenant256.entando.com", "", "context2");
        assertEquals("primary", tenantCode);
        tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "tenant2.entando.com", "", "context256");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByNotExistingXForwardedHostHeaderAndExistingHostAndContextShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant256.entando.com", "tenant2.entando.com", "", "context2");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingServletNameAndContextShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "tenant1.entando.com", "context1");
        assertEquals("tenant1", tenantCode);
    }

    @Test
    void getByNotExistingServletNameOrContextShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "tenant256.entando.com", "context1");
        assertEquals("primary", tenantCode);
        tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "tenant2.entando.com", "context256");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingServletNameAndEmptyContextShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "test3.entando.com", "");
        assertEquals("tenant3", tenantCode);
    }

    @Test
    void getByExistingServletNameAndContextNotDefinedShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "test4.entando.com", null);
        assertEquals("tenant4", tenantCode);
    }


    @Test
    void getPrimaryIfConfigNull() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(null, "", "tenant2.entando.com", "", "", "context1");
        assertEquals("primary", tenantCode);
    }

    private String getTenantConfigMock(String tenantName, String fqdns, String context) {
        String contextConfig = context != null ? ("\"context\":\"" + context + "\",") : "";
        return "{"
                + "\"dbMaxTotal\":\"1\","
                + "\"tenantCode\":\"" + tenantName + "\","
                + "\"initializationAtStartRequired\":\"false\","
                + "\"fqdns\":\" " + fqdns + "\" ,"
                + contextConfig
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