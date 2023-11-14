package org.entando.kubernetes.config.tenant;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.FileSystemResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;
import org.entando.kubernetes.liquibase.helper.UpdateUtils;
import org.springframework.boot.jdbc.DatabaseDriver;

@Slf4j
public class TenantLiquibaseMigration {

    public static final String CHANGELOG_FILE_NAME = "db.changelog-master.yaml";
    private static final FileSystemResourceAccessor resourceAccessor = new FileSystemResourceAccessor(
            new File("/tmp/db/changelog/"));

    public List<ChangeSet> migrate(List<TenantConfigDTO> tenantConfigs) throws Exception {
        final Contexts standard = new Contexts("standard");
        List<ChangeSet> pendingChangeset = new ArrayList<>();

        for (TenantConfigDTO config : tenantConfigs) {
            if (config instanceof PrimaryTenantConfig) {
                continue;
            }
            log.info("Creating (or updating) tenant database '{}' DB URL is {}", config.getTenantCode(), config.getDeDbUrl());
            Database database = createTenantDatasource(config);

            try (Liquibase liquibase = new Liquibase(CHANGELOG_FILE_NAME, resourceAccessor, database)) {
                liquibase.update(standard, new LabelExpression());
                List<ChangeSet> currentPendingChangeSet = liquibase.listUnrunChangeSets(standard, new LabelExpression());
                pendingChangeset.addAll(currentPendingChangeSet);
                log.info("updating of tenant db '{}' completed ({} changeSet not applied)", config.getTenantCode(),
                        currentPendingChangeSet.size());
            }
        }
        return pendingChangeset;
    }

    private Database createTenantDatasource(TenantConfigDTO config) throws Exception {
        final String driver = DatabaseDriver.fromJdbcUrl(config.getDeDbUrl()).getDriverClassName();
        final String schema = UpdateUtils.getSchemaFromJdbc(config.getDeDbUrl());

        Database database = DatabaseFactory.getInstance().openDatabase(
                config.getDeDbUrl(),
                config.getDeDbUsername(),
                config.getDeDbPassword(),
                driver,
                null, null, null, resourceAccessor);

        if (StringUtils.isNotBlank(schema)) {
            database.setLiquibaseSchemaName(schema);
            database.setDefaultSchemaName(schema);
            log.info("Selecting schema {} for migration", schema);
        }
        return database;
    }

}
