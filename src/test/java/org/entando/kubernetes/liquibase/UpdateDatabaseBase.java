package org.entando.kubernetes.liquibase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.hamcrest.Matchers;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class UpdateDatabaseBase {

    public static final String USERNAME = "testuser";

    public static final String PASSWORD = "testpassword";

    public static final String POSTGRES_USERNAME = System.getenv().getOrDefault("POSTGRES_USER", USERNAME);
    public static final String POSTGRES_PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", PASSWORD);

    public static final String MYSQL_USERNAME = System.getenv().getOrDefault("MYSQL_USER", USERNAME);
    public static final String MYSQL_PASSWORD = System.getenv().getOrDefault("MYSQL_PASSWORD", PASSWORD);

    public static final String ORACLE_USERNAME = System.getenv().getOrDefault("ORACLE_USER", USERNAME);
    public static final String ORACLE_PASSWORD = System.getenv().getOrDefault("ORACLE_PASSWORD", PASSWORD);

    public TenantConfigRwDto getTenantForTest(JdbcDatabaseContainer<?> container,  String schema) {
        TenantConfigRwDto cfg = new TenantConfigRwDto();
        StringBuilder jdbc = new StringBuilder(container.getJdbcUrl());

        if (StringUtils.isNotBlank(schema)) {

            if (container.getJdbcUrl().contains("?")) {
                jdbc.append("&");
            } else {
                jdbc.append("?");
            }
            jdbc.append("currentSchema=");
            jdbc.append(schema);
        }
        cfg.setDeDbUrl(jdbc.toString());
        cfg.setTenantCode("TestTenant");
        cfg.setDeDbUsername(USERNAME);
        cfg.setDeDbPassword(PASSWORD);
        return cfg;
    }

    public void assertValidLiquibaseInstance(JdbcDatabaseContainer<?> container, String sql) throws Exception {
        Class.forName(container.getDriverClassName());
        try (Connection connection = DriverManager.getConnection(container.getJdbcUrl(), USERNAME, PASSWORD)) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(sql)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertThat(resultSet.getRow(), Matchers.greaterThan(0));
                }
            }
        } catch (Exception e) {
            throw e;
        }
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
