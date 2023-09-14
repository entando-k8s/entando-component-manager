package org.entando.kubernetes.service.update;

import static org.entando.kubernetes.model.common.EntandoMultiTenancy.PRIMARY_TENANT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.PostConstruct;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UpdateDatabase implements IUpdateDatabase {

    private final List<TenantConfigDTO> tenantConfigs;
    private File changelog;


    public UpdateDatabase(@Qualifier("tenantConfigs") List<TenantConfigDTO> tenantConfigs) {
        this.tenantConfigs = tenantConfigs;
    }

    @PostConstruct
    public void checkOnStart() {
        log.info("Starting schema update check...");
        // copy the Liquibase resources in a safe place
        copyLiquibaseResources();
        checkForDbSchemaUpdate();
        log.info("schema update check completed");
    }

    public void copyLiquibaseResources() {
        try {
            ResourcePatternResolver resourcePatResolver = new PathMatchingResourcePatternResolver();
            Resource[] allResources = resourcePatResolver.getResources("classpath:db/**/*.yaml");
            for (Resource resource: allResources) {
                String uri = resource.getURI().toString();
                uri = uri.substring(uri.lastIndexOf("/db/"));
                String tmpFolder = System.getProperty("java.io.tmpdir");
                Path destinationFile = Path.of(tmpFolder, uri);

                if (destinationFile.getFileName().endsWith("db.changelog-master.yaml")) {
                    changelog = destinationFile.toFile();
                }
                FileUtils.copyInputStreamToFile(resource.getInputStream(), destinationFile.toFile());
            }
        } catch (IOException e) {
            log.error("Error copying Liquibase resources", e);
        }
    }

    private void checkForDbSchemaUpdate() {
        tenantConfigs
                .stream()
                .filter(cfg -> !cfg.getTenantCode().equals(PRIMARY_TENANT))
                .forEach(this::updateTenantDatabase);
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

    // FIXME when https://github.com/liquibase/liquibase/pull/2353 is fixed use this method
    /*
    private Liquibase createLiquibaseFromTenantDefinition(TenantConfigDTO tenantConfig)
            throws DatabaseException {
        Database database = createTenantDatasource(tenantConfig);
        return new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);
    } */

    private Liquibase createLiquibaseFromTenantDefinition(TenantConfigDTO tenantConfig)
            throws DatabaseException {
        Database database = createTenantDatasource(tenantConfig);
        String changeLogFilePath =  changelog.getAbsolutePath();
        String changelogPath = changeLogFilePath.substring(0, changeLogFilePath.lastIndexOf('/'));
        log.info("Database configuration for tenant '{}':\n\turl: {}\n\tusername: {}", tenantConfig.getTenantCode(),
                tenantConfig.getDeDbUrl(), tenantConfig.getDeDbUsername());
        return new Liquibase("db.changelog-master.yaml", new FileSystemResourceAccessor(new File(changelogPath)), database);
    }


    @Override
    public boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws LiquibaseException {
        try (Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig)) {
            return (!liquibase.listUnrunChangeSets(null, null).isEmpty());
        }
    }

    @Override
    public void updateTenantDatabase(TenantConfigDTO tenantConfig) {
        log.info("Checking tenant '{}' for schema update", tenantConfig.getTenantCode());
        try (Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig)) {
            if (!liquibase.listUnrunChangeSets(null, null).isEmpty()) {
                log.info("Applying database updates to tenant '{}'", tenantConfig.getTenantCode());
                liquibase.update("");
                log.info("Schema update completed for tenant '{}'", tenantConfig.getTenantCode());
            } else {
                log.info("No database update available for tenant '{}'", tenantConfig.getTenantCode());
            }
        } catch (LiquibaseException e) {
            log.error("error executing tenant '" + tenantConfig.getTenantCode() + "' database update", e);
        }
    }

}
