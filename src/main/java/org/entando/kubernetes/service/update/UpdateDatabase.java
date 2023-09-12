package org.entando.kubernetes.service.update;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UpdateDatabase implements IUpdateDatabase {

    //    public static final String CHANGELOG_MASTER_YAML = "classpath:db/changelog/db.changelog-master.yaml";
    public static final String CHANGELOG_MASTER_YAML = "classpath:db/changelog/db.changelog-master.yaml";
    private final DataSource dataSource;
    private final List<TenantConfigDTO> tenantConfigs;
    private final Resource changelog;


    public UpdateDatabase(DataSource dataSource, List<TenantConfigDTO> tenantConfigs, ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.tenantConfigs = tenantConfigs;
        this.changelog = resourceLoader.getResource(CHANGELOG_MASTER_YAML);
        if (!changelog.exists()) {
            log.error("Liquibase changelog file not found {} ", CHANGELOG_MASTER_YAML);
            throw new RuntimeException("Invalid Liquibase master changelog!");
        }
    }

    @PostConstruct
    public void testOnStart() {
        log.info("Starting schema update check...");
        checkForDbSchemaUpdate();
        log.info("schema update check completed");
    }

    private void checkForDbSchemaUpdate() {
        try {
            final String actualJdbcUrl = dataSource.getConnection().getMetaData().getURL();
            tenantConfigs
                    .stream()
                    .peek(cfg -> log.debug("actual jdbcUrl {} {}", cfg.getDeDbUrl(), actualJdbcUrl))
                    .filter(cfg -> !cfg.getDeDbUrl().contains(actualJdbcUrl))
                    .forEach(cfg -> {
                        try {
                            updateTenantDatabase(cfg);
                        } catch (IOException e) {
                            log.error("IOException during schema update for tenant " + cfg.getTenantCode(), e);
                            throw new RuntimeException(e);
                        } catch (LiquibaseException e) {
                            log.error("Liquibase exception during schema update for tenant " + cfg.getTenantCode(), e);
                            throw new RuntimeException(e);
                        }
                    });
        } catch (SQLException e) {
            log.error("error updating database", e);
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

    private Liquibase createLiquibaseFromTenantDefinition(TenantConfigDTO tenantConfig)
            throws DatabaseException, IOException {
        Database database = createTenantDatasource(tenantConfig);
        String changeLogFilePath =  changelog.getFile().getAbsolutePath();
        String changelogPath = changeLogFilePath.substring(0, changeLogFilePath.lastIndexOf('/'));
        log.debug("Path of the master changelog {} ", changelogPath);
        return new Liquibase("db.changelog-master.yaml", new FileSystemResourceAccessor(new File(changelogPath)), database);
    }

    @Override
    public boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException {
        try (Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig)) {
            return (liquibase.listUnrunChangeSets(null, null).size() > 0);
        }
    }

    @Override
    public void updateTenantDatabase(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException {
        log.info("Checking tenant {} for schema update", tenantConfig.getTenantCode());
        try (Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig)) {
            if (liquibase.listUnrunChangeSets(null, null).size() > 0) {
                log.debug("Applying database updates to tenant {}", tenantConfig.getTenantCode());
                liquibase.update("");
                log.debug("Schema update completed for tenant {}", tenantConfig.getTenantCode());
            } else {
                log.debug("No database update available for tenant {}", tenantConfig.getTenantCode());
            }
        }
    }

}
