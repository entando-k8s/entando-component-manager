package org.entando.kubernetes.liquibase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.entando.kubernetes.liquibase.TempTest.TenantConfigRwDto;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
public class TempTest2 {

    @Autowired
    private IUpdateDatabase updateDatabase;

    @Autowired
    private ResourceLoader resourceLoader;

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
        // Puoi ottenere l'URL JDBC del database Testcontainers con:
        String jdbcUrl = postgreSQLContainer.getJdbcUrl();
        // Configura la connessione al database nel tuo codice di test
        System.out.println("\n****\n****\n****\n\t" + jdbcUrl);
        System.out.println(">> " + USERNAME);
        System.out.println(">> " + PASSWORD);
    }

    @Test
    void testMe() {
        assertNotNull(updateDatabase);

        try {
            //            Resource changelog = resourceLoader.getResource(
            //                    "classpath:db/changelog/db.changelog-slave.yaml");

            TenantConfigRwDto cfg = new TenantConfigRwDto();


            cfg.setDeDbUrl(postgreSQLContainer.getJdbcUrl());
            cfg.setDeDbUsername(USERNAME);
            cfg.setDeDbPassword(PASSWORD);

            Resource changelog = resourceLoader.getResource(
                    "classpath:db/changelog/db.changelog-master.yaml");

            updateDatabase.updateTenantDatabase(cfg, changelog.getFile().getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Data
    @EqualsAndHashCode
    @ToString(exclude = {"deDbPassword"})
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class TenantConfigRwDto extends TenantConfigDTO {

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
