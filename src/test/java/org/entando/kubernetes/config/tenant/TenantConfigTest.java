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

        String input = "[{\"dbMaxTotal\":\"5\",\"tenantCode\":\"tenant1\",\"initializationAtStartRequired\":\"false\",\"fqdns\":\"mock-fqdns\""
                + ",\"kcEnabled\":true,\"kcAuthUrl\":\"mock-auth-url\",\"kcRealm\":\"tenant1\","
                +  "\"kcClientId\":\"mock-client-id\",\"kcClientSecret\":\"mock-client-secret\","
                +  "\"kcPublicClientId\":\"mock\",\"kcSecureUris\":\"\",\"kcDefaultAuthorizations\":\"\",\"dbDriverClassName\":\"org.postgresql.Driver\","
                +  "\"dbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1\","
                +  "\"dbUsername\":\"username\",\"dbPassword\":\"password\",\"cdsPublicUrl\":\"mock\",\"cdsPrivateUrl\":\"mock\","
                +  "\"cdsPath\":\"api/v1\",\"solrAddress\":\"mock\",\"solrCore\":\"tenant1\"},"

                + "{\"dbMaxTotal\":\"5\",\"tenantCode\":\"tenant2\",\"initializationAtStartRequired\":\"false\",\"fqdns\":\"mock-fqdns\","
                +  "\"kcEnabled\":true,\"kcAuthUrl\":\"mock\",\"kcRealm\":\"tenant2\",\"kcClientId\":\"mock\",\"kcClientSecret\":\"mock\","
                +  "\"kcPublicClientId\":\"mock\",\"kcSecureUris\":\"\",\"kcDefaultAuthorizations\":\"\",\"dbDriverClassName\":\"org.postgresql.Driver\","
                +  "\"dbUrl\":\"mock\",\"dbUsername\":\"username\",\"dbPassword\":\"password\",\"cdsPublicUrl\":\"mock\","
                +  "\"cdsPrivateUrl\":\"mock\",\"cdsPath\":\"api/v1\",\"solrAddress\":\"mock\",\"solrCore\":\"tenant2\"}]";

        TenantConfig tenantConfiguration = new TenantConfig(input, objectMapper);
        List<TenantConfigDTO> tenantConfigs = tenantConfiguration.tenantConfigs();

        assertThat(tenantConfigs.size()).isEqualTo(2);

        TenantConfigDTO tenant1 = tenantConfigs.get(0);
        assertThat(tenant1.getTenantCode()).isEqualTo("tenant1");
        assertThat(tenant1.getDbMaxTotal()).isEqualTo(5);
        assertThat(tenant1.isInitializationAtStartRequired()).isEqualTo(false);
        assertThat(tenant1.getFqdns()).isEqualTo("mock-fqdns");
        assertThat(tenant1.isKcEnabled()).isEqualTo(true);
        assertThat(tenant1.getKcAuthUrl()).isEqualTo("mock-auth-url");
        assertThat(tenant1.getKcRealm()).isEqualTo("tenant1");
        assertThat(tenant1.getKcClientId()).isEqualTo("mock-client-id");
        assertThat(tenant1.getKcSecureUris()).isEqualTo("");
        assertThat(tenant1.getKcDefaultAuthorizations()).isEqualTo("");
        assertThat(tenant1.getDbDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(tenant1.getDbUrl()).isEqualTo("jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1");
        assertThat(tenant1.getDbUsername()).isEqualTo("username");
        assertThat(tenant1.getDbPassword()).isEqualTo("password");
        assertThat(tenant1.getCdsPublicUrl()).isEqualTo("mock");
        assertThat(tenant1.getCdsPrivateUrl()).isEqualTo("mock");
        assertThat(tenant1.getCdsPath()).isEqualTo("api/v1");
        assertThat(tenant1.getSolrAddress()).isEqualTo("mock");
        assertThat(tenant1.getSolrCore()).isEqualTo("tenant1");

        TenantConfigDTO tenant2 = tenantConfigs.get(1);
        assertThat(tenant2.getTenantCode()).isEqualTo("tenant2");
        assertThat(tenant2.getDbMaxTotal()).isEqualTo(5);
        assertThat(tenant2.isInitializationAtStartRequired()).isEqualTo(false);
        assertThat(tenant2.getFqdns()).isEqualTo("mock-fqdns");
        assertThat(tenant2.isKcEnabled()).isEqualTo(true);
        assertThat(tenant2.getKcAuthUrl()).isEqualTo("mock");
        assertThat(tenant2.getKcRealm()).isEqualTo("tenant2");
        assertThat(tenant2.getKcClientId()).isEqualTo("mock");
        assertThat(tenant2.getKcClientSecret()).isEqualTo("mock");
        assertThat(tenant2.getKcPublicClientId()).isEqualTo("mock");
        assertThat(tenant2.getKcSecureUris()).isEqualTo("");
        assertThat(tenant2.getKcDefaultAuthorizations()).isEqualTo("");
        assertThat(tenant2.getDbDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(tenant2.getDbUrl()).isEqualTo("mock");
        assertThat(tenant2.getDbUsername()).isEqualTo("username");
        assertThat(tenant2.getDbPassword()).isEqualTo("password");
        assertThat(tenant2.getCdsPublicUrl()).isEqualTo("mock");
        assertThat(tenant2.getCdsPrivateUrl()).isEqualTo("mock");
        assertThat(tenant2.getCdsPath()).isEqualTo("api/v1");
        assertThat(tenant2.getSolrAddress()).isEqualTo("mock");
        assertThat(tenant2.getSolrCore()).isEqualTo("tenant2");

    }

    @Test
    void shouldReturnErrorWhenParseInvalidTenantConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Invalid JSON data (missing closing bracket)
        String invalidInput = "[{\"dbMaxTotal\":\"5\"";
        TenantConfig tenantConfiguration = new TenantConfig(invalidInput, objectMapper);

        assertThrows(EntandoComponentManagerException.class, () -> tenantConfiguration.tenantConfigs());
    }

}