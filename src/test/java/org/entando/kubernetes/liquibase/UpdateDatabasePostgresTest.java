package org.entando.kubernetes.liquibase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DatabaseDriver;
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
class UpdateDatabasePostgresTest {

    public static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    @Autowired
    private IUpdateDatabase updateDatabase;

    private static final String USERNAME = System.getenv().getOrDefault("POSTGRES_USER", "testuser");
    private static final String PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", "testpassword");
    private static final String DATABASE = System.getenv().getOrDefault("POSTGRES_DATABASE", "mypersonaltestdb");


    @Container
    private static final PostgreSQLContainer<?> referenceDatabase = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    @Container
    private static final PostgreSQLContainer<?> targetDatabase = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

    @BeforeAll
    static void beforeAll() {
        referenceDatabase.start();
        targetDatabase.start();
    }

    @AfterAll
    static void closeContainers() {
        referenceDatabase.close();
        targetDatabase.close();
        referenceDatabase.stop();
        targetDatabase.stop();
    }

    @Test
    void testUpdateDatabaseWithMasterChangelog() throws IOException, LiquibaseException, ParserConfigurationException {
        TenantConfigRwDto cfg = getTenantForTest(referenceDatabase);

        assertTrue(updateDatabase.isTenantDbUpdatePending(cfg));
        updateDatabase.updateTenantDatabase(cfg);
        assertFalse(updateDatabase.isTenantDbUpdatePending(cfg));

        final String DIFF_CHANGELOG_FILE = "onTheFlyUpdate.xml";
        updateDatabase.generateDiff(getTenantForTest(referenceDatabase),
                getTenantForTest(targetDatabase), DIFF_CHANGELOG_FILE);
        File changelog = new File(TMP_DIR + File.separator + DIFF_CHANGELOG_FILE);
        assertTrue(changelog.exists());
        testChangeSet(DIFF_CHANGELOG_FILE, targetDatabase, false);

        // update
        updateDatabase.updateDatabase(getTenantForTest(targetDatabase), DIFF_CHANGELOG_FILE);

        // check the diff again
        final String NO_DIFF_CHANGELOG_FILE = "lastStep.xml";
        updateDatabase.generateDiff(getTenantForTest(referenceDatabase),
                getTenantForTest(targetDatabase), NO_DIFF_CHANGELOG_FILE);
        File noDiffChangelog = new File(TMP_DIR + File.separator + NO_DIFF_CHANGELOG_FILE);
        assertTrue(noDiffChangelog.exists());

        testChangeSet(NO_DIFF_CHANGELOG_FILE, targetDatabase, true);
    }

    @Test
    void evaluateDiffBetweenDatabases() throws LiquibaseException, ParserConfigurationException, IOException {
        final String DIFF_CHANGELOG_FILE = "diff.xml";

        updateDatabase.generateDiff(getTenantForTest(referenceDatabase),
                getTenantForTest(targetDatabase), DIFF_CHANGELOG_FILE);
        File changelog = new File(TMP_DIR + File.separator + DIFF_CHANGELOG_FILE);
        assertTrue(changelog.exists());
    }

    @Test
    void evaluateDiffWithCmDatabase() throws LiquibaseException, ParserConfigurationException, IOException, SQLException {
        final String DIFF_CHANGELOG_FILE = "diff2.xml";

        updateDatabase.generateDiff(getTenantForTest(targetDatabase), DIFF_CHANGELOG_FILE);
        File changelog = new File(TMP_DIR + File.separator + DIFF_CHANGELOG_FILE);
        assertTrue(changelog.exists());
        testChangeSet(DIFF_CHANGELOG_FILE, targetDatabase, false);
    }

    @Test
    void testUpdateTenant() throws Exception {
        final String DIFF_CHANGELOG_FILE = "TestTenant.xml";
        final PostgreSQLContainer<?> yetAnotherDatabase = new PostgreSQLContainer<>("postgres:14")
                .withDatabaseName(DATABASE)
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
        yetAnotherDatabase.start();
        try {
            updateDatabase.updateTenantDatabaseByDiff(getTenantForTest(yetAnotherDatabase));

            File changelog = new File(TMP_DIR + File.separator + DIFF_CHANGELOG_FILE);
            assertTrue(changelog.exists());
            testChangeSet(DIFF_CHANGELOG_FILE, yetAnotherDatabase, false);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            yetAnotherDatabase.close();
            yetAnotherDatabase.stop();
        }
    }

    @NotNull
    private static TenantConfigRwDto getTenantForTest(PostgreSQLContainer<?> container) {
        TenantConfigRwDto cfg = new TenantConfigRwDto();

        cfg.setTenantCode("TestTenant");
        cfg.setDeDbUrl(container.getJdbcUrl());
        cfg.setDeDbUsername(USERNAME);
        cfg.setDeDbPassword(PASSWORD);
        return cfg;
    }

    private void testChangeSet(String changelogFile, PostgreSQLContainer<?> database, boolean isZero) throws LiquibaseException {
        final String TMP_DIR = System.getProperty("java.io.tmpdir");
        final Database targetDb = createTenantDatasource(getTenantForTest(database));
        final List<ChangeSet> changesets;
        try (Liquibase liquibase = new Liquibase(changelogFile, new FileSystemResourceAccessor(new File(TMP_DIR)), targetDb)) {
            changesets = liquibase.getDatabaseChangeLog().getChangeSets();
        }

        if (isZero) {
            assertThat(changesets, empty());
        } else {
            assertThat(changesets, hasSize(greaterThan(0)));
        }
    }

    private Database createTenantDatasource(TenantConfigDTO config) throws DatabaseException {
        final String driver = DatabaseDriver.fromJdbcUrl(config.getDeDbUrl()).getDriverClassName();
        ResourceAccessor resourceAccessor = new FileSystemResourceAccessor();

        return DatabaseFactory.getInstance().openDatabase(
                config.getDeDbUrl(),
                config.getDeDbUsername(),
                config.getDeDbPassword(),
                driver,
                null, null, null, resourceAccessor);
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
