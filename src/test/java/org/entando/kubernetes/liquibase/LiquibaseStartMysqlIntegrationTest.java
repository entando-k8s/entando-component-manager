package org.entando.kubernetes.liquibase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TenantConfiguration;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class,
                TenantConfiguration.class
        })
@ActiveProfiles("testdb")
@Testcontainers
@Slf4j
@Tag("component")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class LiquibaseStartMysqlIntegrationTest {

    private static final String CONTAINER_IMAGE = System.getenv()
            .getOrDefault("MYSQL_CONTAINER_IMAGE", "mysql:8");
    private static final String USERNAME = System.getenv().getOrDefault("MYSQL_USER", "testuser");
    private static final String PASSWORD = System.getenv().getOrDefault("MYSQL_PASSWORD", "testuser");
    private static final String DATABASE = System.getenv().getOrDefault("MYSQL_DATABASE", "testdb");

    private static Properties propsBackup;
    public static MySQLContainer db = new MySQLContainer(
            DockerImageName.parse(CONTAINER_IMAGE).asCompatibleSubstituteFor("mysql")).withDatabaseName(
            DATABASE).withUsername(USERNAME).withPassword(PASSWORD);

    static {
        propsBackup = new Properties(System.getProperties());
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("root").setLevel(Level.INFO);
        db.start();
        log.debug("calculated jdbc url:'{}'", db.getJdbcUrl());
        System.setProperty("spring.datasource.url", db.getJdbcUrl());
        System.setProperty("spring.datasource.driverClassName", "com.mysql.cj.jdbc.Driver");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
        System.setProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        System.setProperty("spring.datasource.username", USERNAME);
        System.setProperty("spring.datasource.password", PASSWORD);
    }

    @AfterAll
    public static void cleanUp() {
        log.debug("cleanUp");
        System.setProperties(propsBackup);
        propsBackup.keySet().forEach(k -> {
            log.trace("key:'{}', value:'{}'", k, propsBackup.get(k));
        });
    }

    @Test
    void testMysqlApplicationStart() {
        log.debug("testMysqlApplicationStart");
    }
}
