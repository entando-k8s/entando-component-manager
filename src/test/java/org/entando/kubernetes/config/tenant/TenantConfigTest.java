package org.entando.kubernetes.config.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Tag("unit")
class TenantConfigTest {
    @Test
    void shouldParseTenantConfig() {
        ObjectMapper objectMapper = new ObjectMapper();

        String input = "[" + getTenantConfigMock("tenant1") + "," + getTenantConfigMock("tenant2") + "]";

        TenantConfig tenantConfiguration = new TenantConfig(input, objectMapper);
        List<TenantConfigDTO> tenantConfigs = tenantConfiguration.tenantConfigs();

        assertThat(tenantConfigs).hasSize(2);

        TenantConfigDTO tenant1 = tenantConfigs.get(0);
        assertTenantConfig(tenant1, "tenant1");

        TenantConfigDTO tenant2 = tenantConfigs.get(1);
        assertTenantConfig(tenant2, "tenant2");

    }

    @Test
    void shouldReturnErrorWhenParseInvalidTenantConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Invalid JSON data (missing closing bracket)
        String invalidInput = "[{\"dbMaxTotal\":\"5\"";
        TenantConfig tenantConfiguration = new TenantConfig(invalidInput, objectMapper);

        assertThrows(EntandoComponentManagerException.class, tenantConfiguration::tenantConfigs);
    }


    private String getTenantConfigMock(String tenantName) {
        return "{\"dbMaxTotal\":\"5\",\"tenantCode\":\"" + tenantName + "\",\"initializationAtStartRequired\":\"false\",\"fqdns\":\"mock-fqdns\""
                + ",\"kcEnabled\":true,\"kcAuthUrl\":\"mock-auth-url\",\"kcRealm\":\"tenant1\","
                +  "\"kcClientId\":\"mock-client-id\",\"kcClientSecret\":\"mock-client-secret\","
                +  "\"kcPublicClientId\":\"mock\",\"kcSecureUris\":\"kcsecureuris\",\"kcDefaultAuthorizations\":\"\",\"dbDriverClassName\":\"org.postgresql.Driver\","
                +  "\"dbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1\","
                +  "\"dbUsername\":\"username\",\"dbPassword\":\"password\",\"cdsPublicUrl\":\"cdspublicurl\",\"cdsPrivateUrl\":\"cdsprivateurl\","
                +  "\"cdsPath\":\"api/v1\",\"solrAddress\":\"solraddress\",\"solrCore\":\"tenant1\","

                +  "\"deDbDriverClassName\": \"org.postgresql.Driver\","
                +  "\"deDbPassword\": \"pwd\","
                +  "\"deDbUrl\": \"jdbc:postgresql://db-address:5432/tenant1_cm?currentSchema=quickstart_dedb_12345\","
                +  "\"deDbUsername\": \"postgres\","
                +  "\"deKcClientId\": \"dekcclientid\","
                +  "\"deKcClientSecret\": \"dekcsecret\""

                + "}";
    }

    private void assertTenantConfig(TenantConfigDTO tenant, String tenantName) {
        assertThat(tenant.getTenantCode()).isEqualTo(tenantName);
        assertThat(tenant.getDbMaxTotal()).isEqualTo(5);
        assertThat(tenant.isInitializationAtStartRequired()).isFalse();
        assertThat(tenant.getFqdns()).isEqualTo("mock-fqdns");
        assertThat(tenant.isKcEnabled()).isTrue();
        assertThat(tenant.getKcAuthUrl()).isEqualTo("mock-auth-url");
        assertThat(tenant.getKcRealm()).isEqualTo("tenant1");
        assertThat(tenant.getKcClientId()).isEqualTo("mock-client-id");
        assertThat(tenant.getKcSecureUris()).isEqualTo("kcsecureuris");
        assertThat(tenant.getKcDefaultAuthorizations()).isEmpty();
        assertThat(tenant.getDbDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(tenant.getDbUrl()).isEqualTo("jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1");
        assertThat(tenant.getDbUsername()).isEqualTo("username");
        assertThat(tenant.getDbPassword()).isEqualTo("password");
        assertThat(tenant.getCdsPublicUrl()).isEqualTo("cdspublicurl");
        assertThat(tenant.getCdsPrivateUrl()).isEqualTo("cdsprivateurl");
        assertThat(tenant.getCdsPath()).isEqualTo("api/v1");
        assertThat(tenant.getSolrAddress()).isEqualTo("solraddress");
        assertThat(tenant.getSolrCore()).isEqualTo("tenant1");

        assertThat(tenant.getDeDbDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(tenant.getDeDbPassword()).isEqualTo("pwd");
        assertThat(tenant.getDeDbUrl()).isEqualTo("jdbc:postgresql://db-address:5432/tenant1_cm?currentSchema=quickstart_dedb_12345");
        assertThat(tenant.getDeDbUsername()).isEqualTo("postgres");
        assertThat(tenant.getDeKcClientId()).isEqualTo("dekcclientid");
        assertThat(tenant.getDeKcClientSecret()).isEqualTo("dekcsecret");
    }

}
