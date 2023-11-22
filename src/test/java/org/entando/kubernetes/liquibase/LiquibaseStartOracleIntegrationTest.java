package org.entando.kubernetes.liquibase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.utils.TenantPrimaryContextJunitExt;
import org.entando.kubernetes.utils.TenantSecurityKeycloakMockServerJunitExt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        })
@ActiveProfiles("testdb")
@Testcontainers
@Slf4j
@Tag("component")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ExtendWith({TenantPrimaryContextJunitExt.class, TenantSecurityKeycloakMockServerJunitExt.class})
class LiquibaseStartOracleIntegrationTest {

    private static final String CONTAINER_IMAGE = System.getenv()
            .getOrDefault("ORACLE_CONTAINER_IMAGE", "gvenzl/oracle-xe:18.4.0-slim");
    private static final String USERNAME = System.getenv().getOrDefault("ORACLE_USER", "testuser");
    private static final String PASSWORD = System.getenv().getOrDefault("ORACLE_PASSWORD", "testuser");
    private static final String DATABASE = System.getenv().getOrDefault("ORACLE_DATABASE", "testdb");
    private static Properties propsBackup;

    public static OracleContainer db = new OracleContainer(
            DockerImageName.parse(CONTAINER_IMAGE).asCompatibleSubstituteFor("oracle")).withDatabaseName(DATABASE)
            .withUsername(USERNAME).withPassword(PASSWORD);

    static {
        propsBackup = new Properties(System.getProperties());
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("root").setLevel(Level.INFO);
        db.start();
        log.debug("calculated jdbc url:'{}'", db.getJdbcUrl());
        System.setProperty("spring.datasource.url", db.getJdbcUrl());
        System.setProperty("spring.datasource.driverClassName", "oracle.jdbc.OracleDriver");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.Oracle12cDialect");
        System.setProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
        System.setProperty("spring.datasource.username", USERNAME);
        System.setProperty("spring.datasource.password", PASSWORD);
    }

    @AfterAll
    public static void cleanUp() {
        log.debug("cleanUp");
        System.setProperties(propsBackup);
    }

    @Test
    void testOracleApplicationStart() {
        log.debug("testOracleApplicationStart");
    }
}
