package org.entando.kubernetes.service.update;

import static org.entando.kubernetes.model.common.EntandoMultiTenancy.PRIMARY_TENANT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UpdateDatabase implements IUpdateDatabase {

    public static final String CHANGELOG_MASTER_YAML = "classpath:db/changelog/db.changelog-master.yaml";
    private final List<TenantConfigDTO> tenantConfigs;

    private File changelog;


    public UpdateDatabase(@Qualifier("tenantConfigs") List<TenantConfigDTO> tenantConfigs, ResourceLoader resourceLoader)
            throws LiquibaseException {
        this.tenantConfigs = tenantConfigs;
        Resource changelog = resourceLoader.getResource(CHANGELOG_MASTER_YAML);
        if (!changelog.exists()) {
            log.error("Liquibase changelog file not found {} ", CHANGELOG_MASTER_YAML);
            throw new LiquibaseException("Invalid Liquibase master changelog!");
        }
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
            Resource[] AllResources = resourcePatResolver.getResources("classpath:db/**/*.yaml");
            for(Resource resource: AllResources) {
                InputStream inputStream = resource.getInputStream();
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
            e.printStackTrace();
        }
    }

    private void checkForDbSchemaUpdate() {
        tenantConfigs
                .stream()
                .filter(cfg -> !cfg.getTenantCode().equals(PRIMARY_TENANT))
                .forEach(cfg -> {
                    try {
                        updateTenantDatabase(cfg);
                    } catch (IOException e) {
                        log.error("IOException during schema update for tenant " + cfg.getTenantCode(), e);
                    } catch (LiquibaseException e) {
                        log.error("Liquibase exception during schema update for tenant " + cfg.getTenantCode(), e);
                    }
                });
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
            throws DatabaseException, IOException {
        Database database = createTenantDatasource(tenantConfig);
        String changeLogFilePath =  changelog.getAbsolutePath();
        String changelogPath = changeLogFilePath.substring(0, changeLogFilePath.lastIndexOf('/'));
        log.debug("Path of the master changelog {} ", changelogPath);
        return new Liquibase("db.changelog-master.yaml", new FileSystemResourceAccessor(new File(changelogPath)), database);
    }


    @Override
    public boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws LiquibaseException, IOException {
        try (Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig)) {
            return (!liquibase.listUnrunChangeSets(null, null).isEmpty());
        }
    }

    @Override
    public void updateTenantDatabase(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException {
        log.info("Checking tenant {} for schema update", tenantConfig.getTenantCode());
        try (Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig)) {
            if (!liquibase.listUnrunChangeSets(null, null).isEmpty()) {
                log.info("Applying database updates to tenant {}", tenantConfig.getTenantCode());
                liquibase.update("");
                log.info("Schema update completed for tenant {}", tenantConfig.getTenantCode());
            } else {
                log.info("No database update available for tenant {}", tenantConfig.getTenantCode());
            }
        }
    }

}
