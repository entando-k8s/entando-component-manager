package org.entando.kubernetes.liquibase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import liquibase.exception.LiquibaseException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.entando.kubernetes.service.update.IUpdateDatabase;
import org.entando.kubernetes.utils.TenantContextJunitExt;
import org.entando.kubernetes.utils.TenantSecurityKeycloakMockServerJunitExt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;


@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        })
@ActiveProfiles("testupdatedb")
@Tag("component")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@Slf4j
@ExtendWith({TenantContextJunitExt.class, TenantSecurityKeycloakMockServerJunitExt.class})
public class UpdateDatabasePostgresTest {

    @Autowired
    private IUpdateDatabase updateDatabase;

    private static final String USERNAME = System.getenv().getOrDefault("POSTGRES_USER", "testuser");
    private static final String PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", "testpassword");
    private static final String DATABASE = System.getenv().getOrDefault("POSTGRES_DATABASE", "mypersonaltestdb");


    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    @BeforeAll
    static void beforeAll() {
        postgreSQLContainer.start();
    }

    @Test
    void testUpdateDatabaseWithMasterChangelog() throws IOException, LiquibaseException {
        TenantConfigRwDto cfg = new TenantConfigRwDto();

        cfg.setDeDbUrl(postgreSQLContainer.getJdbcUrl());
        cfg.setDeDbUsername(USERNAME);
        cfg.setDeDbPassword(PASSWORD);

        assertTrue(updateDatabase.isTenantDbUpdatePending(cfg));
        updateDatabase.updateTenantDatabase(cfg);
        assertFalse(updateDatabase.isTenantDbUpdatePending(cfg));
    }


    @Data
    @EqualsAndHashCode
    @ToString(exclude = {"deDbPassword"})
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TenantConfigRwDto extends TenantConfigDTO {

        private String tenantCode;
        private String fqdns;

        private String kcAuthUrl;
        private String kcRealm;
        private String deKcClientId;
        private String deKcClientSecret;

        private String deDbUrl;
        private String deDbUsername;
        private String deDbPassword;
    }

}
