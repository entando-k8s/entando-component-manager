package org.entando.kubernetes.liquibase;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.Collections;
import java.util.List;
import liquibase.changelog.ChangeSet;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.tenant.TenantLiquibaseMigration;
import org.entando.kubernetes.utils.TenantContextJunitExt;
import org.entando.kubernetes.utils.TenantSecurityKeycloakMockServerJunitExt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
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
class UpdateDatabaseMySQLTest extends UpdateDatabaseBase {

    private static final String DATABASE = System.getenv().getOrDefault("MYSQL_DATABASE", "mypersonaltestdb");

    @Container
    private static final MySQLContainer<?> targetDatabase = new MySQLContainer<>("mysql:8.0.26")
            .withDatabaseName(DATABASE)
            .withUsername(MYSQL_USERNAME)
            .withPassword(MYSQL_PASSWORD);

    @BeforeAll
    static void beforeAll() {
        targetDatabase.start();
    }

    @AfterAll
    static void closeContainers() {
        targetDatabase.close();
        targetDatabase.stop();
    }

    @Test
    void testUpdateDatabaseWithMasterChangelog() throws Exception {
        String dbDir = moveResources(TMP_DB_FOLDER);
        TenantConfigRwDto cfg = getTenantForTest(targetDatabase, null);
        List<ChangeSet> pendingChangeset = new TenantLiquibaseMigration().migrate(Collections.singletonList(cfg), dbDir);
        assertThat(pendingChangeset, empty());
        assertValidLiquibaseInstance(targetDatabase, "SELECT id FROM DATABASECHANGELOG");
    }


}
