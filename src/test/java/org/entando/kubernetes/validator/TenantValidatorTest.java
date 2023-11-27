package org.entando.kubernetes.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class TenantValidatorTest {

    @Test
    void testInvalidConfiguration1() throws JsonProcessingException {
        Optional<Map<String, List<String>>> opt = TenantValidator
                .validate(getConfigFromJson(INVALID_TENANT_CONFIG1))
                .getValidationErrorMap();
        assertNotNull(opt);
        assertTrue(opt.isPresent());
        Map<String, List<String>> map = opt.get();
        assertFalse(map.isEmpty());
        MatcherAssert.assertThat(map, Matchers.allOf(
                Matchers.hasKey("tenant1"),
                Matchers.hasKey("tenant3"),
                Matchers.hasKey("tenant2")
        ));
        MatcherAssert.assertThat(map, Matchers.not(Matchers.hasKey("tenant4")));
        List<String> errors = map.get("tenant2");
        assertFalse(errors.isEmpty());
        MatcherAssert.assertThat(errors, Matchers.containsInAnyOrder(
                "deDbUsername: missing configuration value",
                "deDbPassword: missing configuration value",
                "deDbUrl: missing configuration value",
                "kcAuthUrl: invalid URL detected 'mock-auth-url'",
                "fqdns: 'mock-fqdns' already used by tenant 'tenant1'",
                "fqdns: invalid value detected 'mock-fqdns'")
        );
    }


    @Test
    void testInvalidDeDBUrl() throws JsonProcessingException {
        Optional<Map<String, List<String>>> opt = TenantValidator
                .validate(getConfigFromJson(INVALID_TENANT_CONFIG_DB_URL))
                .getValidationErrorMap();
        assertNotNull(opt);
        assertTrue(opt.isPresent());
        Map<String, List<String>> map = opt.get();
        assertFalse(map.isEmpty());
        MatcherAssert.assertThat(map, Matchers.allOf(
                Matchers.hasKey("tenant1")
        ));
        List<String> errors = map.get("tenant1");
        assertFalse(errors.isEmpty());
        MatcherAssert.assertThat(errors, Matchers.contains(
                "deDbUrl: invalid DB Url")
        );
    }

    @Test
    void testInvalidConfiguration2() throws JsonProcessingException {
        Optional<Map<String, List<String>>> opt = TenantValidator
                .validate(getConfigFromJson(INVALID_TENANT_CONFIG2))
                .getValidationErrorMap();
        assertNotNull(opt);
        assertTrue(opt.isPresent());
        Map<String, List<String>> map = opt.get();
        assertFalse(map.isEmpty());
        MatcherAssert.assertThat(map, Matchers.hasKey("tenant1"));
        List<String> errors = map.get("tenant1");
        assertFalse(errors.isEmpty());
        MatcherAssert.assertThat(errors, Matchers.containsInAnyOrder(
                "deDbUsername: missing configuration value",
                "deDbPassword: missing configuration value",
                "deDbUrl: missing configuration value",
                "kcAuthUrl: invalid URL detected 'mock-auth-url'",
                "tenant with FQDNs' mock-fqdns' is using the same tenant id (tenant1)",
                "fqdns: invalid value detected 'mock-fqdns'")
        );
    }

    private List<TenantConfigDTO> getConfigFromJson(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    // Tenant4 is good!
    public static final String INVALID_TENANT_CONFIG1 = "[\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant1\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"mock-auth-url\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant2\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"mock-auth-url\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant4\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns.tld\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenenats.k8s-server.org/auth\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"dbPassword\":\"password\",\n"
            + "      \"cdsPublicUrl\":\"cdspublicurl\",\n"
            + "      \"cdsPrivateUrl\":\"cdsprivateurl\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\",\n"
            + "      \"deDbPassword\":\"password\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant3\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"mock-auth-url\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\"\n"
            + "   }\n"
            + "]";

    public static final String INVALID_TENANT_CONFIG2 = "[\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant1\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns.tld\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenenats.k8s-server.org/auth\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"dbPassword\":\"password\",\n"
            + "      \"cdsPublicUrl\":\"cdspublicurl\",\n"
            + "      \"cdsPrivateUrl\":\"cdsprivateurl\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\",\n"
            + "      \"deDbPassword\":\"password\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://db-address:5432/tenant1_cm?currentSchema=quickstart_dedb_12345\",\n"
            + "      \"deKcClientId\":\"dekcclientid\",\n"
            + "      \"deKcClientSecret\":\"dekcsecret\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant1\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"mock-auth-url\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\",\n"
            + "      \"deKcClientId\":\"dekcclientid\",\n"
            + "      \"deKcClientSecret\":\"dekcsecret\"\n"
            + "   }\n"
            + "]";

    public static final String INVALID_TENANT_CONFIG_DB_URL = "[\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant1\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"tenant1.test-entando.com\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenant1.test-entando.com/auth\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\",\n"
            + "      \"deDbUrl\":\"jdbc:test://test:5432/tenant1\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"deDbPassword\":\"password\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant2\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"tenant2.test-entando.com\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenant2.test-entando.com/auth\",\n"
            + "      \"kcRealm\":\"tenant1\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant1\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://test:5432/tenant2\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"deDbPassword\":\"password\"\n"
            + "   }\n"
            + "]";
}
