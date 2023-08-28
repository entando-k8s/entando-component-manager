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
        config.append(getTenantConfigMock("tenant1", "test.entando.com, test2.entando.com, tenant1.entando.com"));
        config.append(",");
        config.append(getTenantConfigMock("tenant2", "tenant2.entando.com, test3.entando.com"));
        config.append(",");
        config.append(getTenantConfigMock("tenant3", "tenant3.entando.com, test4.entando.com"));
        config.append("]");
        String tenantsConfig = config.toString();
        configDTOList = (new ObjectMapper()).readValue(tenantsConfig,
                new TypeReference<List<TenantConfigDTO>>() {
                });
    }

    @Test
    void shouldReturnTheTenantCodeFromExistingXEntandoCustomHeader() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "xtenant", "tenant2.entando.com", "", "");
        assertEquals("xtenant", tenantCode);
    }

    @Test
    void getByExistingXForwardedHostHeaderShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant2.entando.com", "", "");
        assertEquals("tenant2", tenantCode);
    }

    @Test
    void getByNotExistingXForwardedHostHeaderShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant256.entando.com", "", "");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingHostHeaderShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "tenant2.entando.com", "");
        assertEquals("tenant2", tenantCode);
    }

    @Test
    void getByNotExistingHostHeaderShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "tenant256.entando.com", "");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByNotExistingXForwardedHostHeaderAndExistingHostShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "tenant256.entando.com", "tenant2.entando.com", "");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getByExistingServletNameShouldReturnTheTenantCode() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "tenant1.entando.com");
        assertEquals("tenant1", tenantCode);
    }

    @Test
    void getByNotExistingServletNameShouldReturnPrimary() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(configDTOList, "", "", "", "tenant256.entando.com");
        assertEquals("primary", tenantCode);
    }

    @Test
    void getPrimaryIfConfigNull() {
        String tenantCode = TenantFilterUtils.fetchTenantCode(null, "", "tenant2.entando.com", "", "");
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