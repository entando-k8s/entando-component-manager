package org.entando.kubernetes.service.update;

import static org.entando.kubernetes.model.common.EntandoMultiTenancy.PRIMARY_TENANT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
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

    private final DataSource referenceDataSource;
    final String tempDir = System.getProperty("java.io.tmpdir");

    public UpdateDatabase(@Qualifier("tenantConfigs") List<TenantConfigDTO> tenantConfigs,
            DataSource referenceDataSource) {
        this.tenantConfigs = tenantConfigs;
        this.referenceDataSource = referenceDataSource;
    }

    @PostConstruct
    public void checkOnStart() throws SQLException, LiquibaseException, ParserConfigurationException, IOException {
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

    private void checkForDbSchemaUpdate()
            throws SQLException, LiquibaseException, ParserConfigurationException, IOException {
        // .forEach(this::updateTenantDatabase);
        for (TenantConfigDTO cfg : tenantConfigs) {
            if (!cfg.getTenantCode().equals(PRIMARY_TENANT)) {
                updateTenantDatabaseByDiff(cfg);
            }
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

    @Override
    public void updateTenantDatabaseByDiff(TenantConfigDTO tenantConfig)
            throws SQLException, LiquibaseException, ParserConfigurationException, IOException {
        final String tmpDiffXmlChangelog = tenantConfig.getTenantCode() + ".xml";

        log.info("Checking tenant '{}' for schema update", tenantConfig.getTenantCode());
        try {
            // step 1: generate diff from current schema to destination
            generateDiff(tenantConfig, tmpDiffXmlChangelog);
            // step 2: apply the changelogs to the tenant DB
            updateDatabase(tenantConfig, tmpDiffXmlChangelog);
            log.info("schema updated completed for tenant '{}'", tenantConfig.getTenantCode());
        } catch (LiquibaseException | SQLException | ParserConfigurationException | IOException e) {
            log.error("error updating tenant schema '{}", tenantConfig.getTenantCode(), e);
            throw e;
        }
    }

    @Override
    public void generateDiff(TenantConfigDTO tenantConfig, String changelog)
            throws SQLException, LiquibaseException, ParserConfigurationException, IOException {
        Database referenceDatabase = getCurrentDatabase();
        Database targetDatabase = createTenantDatasource(tenantConfig);

        try {
            performDiff(referenceDatabase, targetDatabase, changelog);
        } catch (LiquibaseException
                 | ParserConfigurationException
                 | IOException t) {
            log.error("error while comparing the databases {}:{} and {}:{}",
                    "primary", referenceDatabase.getConnection().getURL(),
                    tenantConfig.getTenantCode(), tenantConfig.getDeDbUrl());
            throw t;
        }
    }

    @Override
    public void generateDiff(TenantConfigDTO reference, TenantConfigDTO target, String changelog)
            throws LiquibaseException, ParserConfigurationException, IOException {

        deleteIfExists(changelog);
        try {
            Database targetDatabase = createTenantDatasource(target);
            Database referenceDatabase = createTenantDatasource(reference);

            performDiff(referenceDatabase, targetDatabase, changelog);
        } catch (LiquibaseException
                 | ParserConfigurationException
                 | IOException t) {
            log.error("error while comparing the databases {}:{} and {}:{}",
                    reference.getTenantCode(), reference.getDeDbUrl(),
                    target.getTenantCode(), target.getDeDbUrl());
            throw t;
        }
    }

    @Override
    public void updateDatabase(TenantConfigDTO targetTenant, String changelog) throws LiquibaseException {
        Database targetDatabase = createTenantDatasource(targetTenant);

        log.info("updating the tenant database {}:{}", targetTenant.getTenantCode(), targetTenant.getDeDbUrl());
        try (Liquibase liquibase = new Liquibase(changelog, new FileSystemResourceAccessor(new File(tempDir)), targetDatabase)) {
            liquibase.clearCheckSums();
            liquibase.update("");
        } catch (LiquibaseException t) {
            log.error("error updating the target database {}:{}", targetTenant.getTenantCode(), targetTenant.getDeDbUrl());
            throw t;
        }
    }

    private void performDiff(Database referenceDatabase, Database targetDatabase, String changelog)
            throws LiquibaseException, ParserConfigurationException, IOException {
        log.info("generating database diff between {} and {}", referenceDatabase.getConnection().getURL(),
                targetDatabase.getConnection().getURL());
        deleteIfExists(changelog);
        try (Liquibase liquibase = new Liquibase("", new FileSystemResourceAccessor(new File(tempDir)), referenceDatabase)) {
            DiffResult diffResult = liquibase.diff(referenceDatabase, targetDatabase, new CompareControl());
            DiffToChangeLog diffChangelog = new DiffToChangeLog(diffResult, new DiffOutputControl());
            final String changelogTmpFile = Path.of(tempDir, changelog).toString();
            log.info("changelog produced in {}", changelogTmpFile);
            diffChangelog.print(changelogTmpFile);
        }
    }

    /**
     * Return the database currently in use.
     * @return the Liquibase Database object
     * @throws SQLException in case of Liquibase error
     * @throws DatabaseException in case of Liquibase error
     */
    private Database getCurrentDatabase() throws SQLException, DatabaseException {
        Connection referenceConnection = referenceDataSource.getConnection();
        return DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(referenceConnection));
    }

    /**
     * Check that a given file exists and deletes it in the case.
     * @param file the file name to check, everything is local to the tmp directory
     */
    private void deleteIfExists(String file) throws IOException {
        Path tmpFile = Path.of(tempDir, file);

        if (tmpFile.toFile().exists()) {
            log.debug("deleting existing file {}", tmpFile);
            Files.delete(tmpFile);
        }
    }

}
