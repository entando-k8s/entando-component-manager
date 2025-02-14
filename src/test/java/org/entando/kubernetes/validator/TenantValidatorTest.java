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
        MatcherAssert.assertThat(map, Matchers.not(Matchers.hasKey("tenant6")));
        List<String> errors = map.get("tenant2");
        assertFalse(errors.isEmpty());
        MatcherAssert.assertThat(errors, Matchers.containsInAnyOrder(
                "deDbUsername: missing configuration value",
                "deDbPassword: missing configuration value",
                "deDbUrl: missing configuration value",
                "kcAuthUrl: invalid URL detected 'mock-auth-url'",
                "The couple fqdns: 'mock-fqdns' - context: 'null' already used by tenant 'tenant1'",
                "fqdns: invalid value detected 'mock-fqdns'")
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
                "tenant with the couple FQDNs: 'mock-fqdns' - context: 'null' is using the same tenant id (tenant1)",
                "fqdns: invalid value detected 'mock-fqdns'")
        );
    }

    @Test
    void testInvalidConfiguration3() throws JsonProcessingException {
        Optional<Map<String, List<String>>> opt = TenantValidator
                .validate(getConfigFromJson(INVALID_TENANT_CONFIG3))
                .getValidationErrorMap();
        assertNotNull(opt);
        assertTrue(opt.isPresent());
        Map<String, List<String>> map = opt.get();
        assertFalse(map.isEmpty());
        MatcherAssert.assertThat(map, Matchers.hasKey("tenant11"));
        List<String> errors = map.get("tenant11");
        assertFalse(errors.isEmpty());
        MatcherAssert.assertThat(errors, Matchers.containsInAnyOrder(
                "The couple fqdns: 'mock-fqdns.tld' - context: 'mock-context-10' already used by tenant 'tenant10'")
        );
    }

    private List<TenantConfigDTO> getConfigFromJson(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    // Tenant4 and Tenant6 are good!
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
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant6\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns.tld\",\n"
            + "      \"context\":\"mock-context\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenenats.k8s-server.org/auth\",\n"
            + "      \"kcRealm\":\"tenant6\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant6\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"dbPassword\":\"password\",\n"
            + "      \"cdsPublicUrl\":\"cdspublicurl\",\n"
            + "      \"cdsPrivateUrl\":\"cdsprivateurl\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant6\",\n"
            + "      \"deDbPassword\":\"password\"\n"
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

    public static final String INVALID_TENANT_CONFIG3 = "[\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant10\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns.tld\",\n"
            + "      \"context\":\"mock-context-10\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenenats.k8s-server.org/auth\",\n"
            + "      \"kcRealm\":\"tenant10\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant10\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"dbPassword\":\"password\",\n"
            + "      \"cdsPublicUrl\":\"cdspublicurl\",\n"
            + "      \"cdsPrivateUrl\":\"cdsprivateurl\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant10\",\n"
            + "      \"deDbPassword\":\"password\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://db-address:5432/tenant10_cm?currentSchema=quickstart_dedb_12345\",\n"
            + "      \"deKcClientId\":\"dekcclientid\",\n"
            + "      \"deKcClientSecret\":\"dekcsecret\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant11\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns.tld\",\n"
            + "      \"context\":\"mock-context-10\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenenats.k8s-server.org/auth\",\n"
            + "      \"kcRealm\":\"tenant11\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant11\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"dbPassword\":\"password\",\n"
            + "      \"cdsPublicUrl\":\"cdspublicurl\",\n"
            + "      \"cdsPrivateUrl\":\"cdsprivateurl\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant11\",\n"
            + "      \"deDbPassword\":\"password\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://db-address:5432/tenant11_cm?currentSchema=quickstart_dedb_12345\",\n"
            + "      \"deKcClientId\":\"dekcclientid\",\n"
            + "      \"deKcClientSecret\":\"dekcsecret\"\n"
            + "   },\n"
            + "   {\n"
            + "      \"dbMaxTotal\":\"5\",\n"
            + "      \"tenantCode\":\"tenant12\",\n"
            + "      \"initializationAtStartRequired\":\"false\",\n"
            + "      \"fqdns\":\"mock-fqdns.tld\",\n"
            + "      \"context\":\"mock-context-12\",\n"
            + "      \"kcEnabled\":true,\n"
            + "      \"kcAuthUrl\":\"https://tenenats.k8s-server.org/auth\",\n"
            + "      \"kcRealm\":\"tenant12\",\n"
            + "      \"kcCmClientId\":\"mock-client-id\",\n"
            + "      \"deKcClientSecret\":\"mock-client-secret\",\n"
            + "      \"kcPublicClientId\":\"mock\",\n"
            + "      \"kcSecureUris\":\"kcsecureuris\",\n"
            + "      \"kcDefaultAuthorizations\":\"\",\n"
            + "      \"dbDriverClassName\":\"org.postgresql.Driver\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant12\",\n"
            + "      \"deDbUsername\":\"username\",\n"
            + "      \"dbPassword\":\"password\",\n"
            + "      \"cdsPublicUrl\":\"cdspublicurl\",\n"
            + "      \"cdsPrivateUrl\":\"cdsprivateurl\",\n"
            + "      \"cdsPath\":\"api/v1\",\n"
            + "      \"solrAddress\":\"solraddress\",\n"
            + "      \"solrCore\":\"tenant12\",\n"
            + "      \"deDbPassword\":\"password\",\n"
            + "      \"deDbUrl\":\"jdbc:postgresql://db-address:5432/tenant12_cm?currentSchema=quickstart_dedb_12345\",\n"
            + "      \"deKcClientId\":\"dekcclientid\",\n"
            + "      \"deKcClientSecret\":\"dekcsecret\"\n"
            + "   }\n"
            + "]";
}
