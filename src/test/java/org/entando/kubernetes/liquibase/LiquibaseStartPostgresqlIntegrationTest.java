package org.entando.kubernetes.liquibase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.TestTenantConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class,
                TestTenantConfiguration.class
        })
@ActiveProfiles("testdb")
@Testcontainers
@Slf4j
@Tag("component")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class LiquibaseStartPostgresqlIntegrationTest {

    //"entando/entando-postgres-rocky:14.1.0"
    private static final String CONTAINER_IMAGE = System.getenv()
            .getOrDefault("POSTGRESQL_CONTAINER_IMAGE", "postgres:14");
    private static final String USERNAME = System.getenv().getOrDefault("POSTGRESQL_USER", "testuser");
    private static final String PASSWORD = System.getenv().getOrDefault("POSTGRESQL_PASSWORD", "testuser");
    private static final String DATABASE = System.getenv().getOrDefault("POSTGRESQL_DATABASE", "testdb");
    private static Properties propsBackup;

    public static PostgreSQLContainer db = new PostgreSQLContainer(
            DockerImageName.parse(CONTAINER_IMAGE).asCompatibleSubstituteFor("postgres")).withDatabaseName(
            DATABASE).withUsername(USERNAME).withPassword(PASSWORD);

    static {
        propsBackup = new Properties(System.getProperties());
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("root").setLevel(Level.INFO);
        db.start();
        log.debug("calculated jdbc url:'{}'", db.getJdbcUrl());
        System.setProperty("spring.datasource.url", db.getJdbcUrl());
        System.setProperty("spring.datasource.driverClassName", "org.postgresql.Driver");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        System.setProperty("spring.datasource.username", USERNAME);
        System.setProperty("spring.datasource.password", PASSWORD);
    }

    @AfterAll
    public static void cleanUp() {
        log.debug("cleanUp");
        System.setProperties(propsBackup);
    }

    @Test
    void testPostgresqlApplicationStart() {
        log.debug("testPostgresqlApplicationStart");
    }
}
