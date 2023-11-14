package org.entando.kubernetes.config.tenant;


import java.util.ArrayList;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;
import org.entando.kubernetes.liquibase.helper.UpdateUtils;
import org.springframework.boot.jdbc.DatabaseDriver;

@Slf4j
public class TenantLiquibaseMigration {

    public static final String CHANGELOG_FILE_NAME = "db.changelog-master.yaml";
    public static final String CLASSPATH_CHANGELOG_FILE_NAME = "classpath:db/changelog/" + CHANGELOG_FILE_NAME;

    public List<ChangeSet> migrate(List<TenantConfigDTO> tenantConfigs, boolean resourcesOnFs) throws Exception {
        final Contexts standard = new Contexts("standard");
        final String changeLogFile = resourcesOnFs ? CHANGELOG_FILE_NAME : CLASSPATH_CHANGELOG_FILE_NAME;
        final AbstractResourceAccessor accessor = getResourceAccessor(resourcesOnFs);
        List<ChangeSet> pendingChangeset = new ArrayList<>();

        for (TenantConfigDTO config : tenantConfigs) {
            if (config instanceof PrimaryTenantConfig) {
                continue;
            }
            log.info("Creating (or updating) tenant database '{}' DB URL is {}", config.getTenantCode(), config.getDeDbUrl());
            Database database = createTenantDatasource(config, resourcesOnFs);

            try (Liquibase liquibase = new Liquibase(changeLogFile, accessor, database);) {
                liquibase.update(standard, new LabelExpression());
                List<ChangeSet> currentPendingChangeSet = liquibase.listUnrunChangeSets(standard);
                pendingChangeset.addAll(currentPendingChangeSet);
                log.info("updating of tenant db '{}' completed ({} changeSet not applied)", config.getTenantCode(),
                        currentPendingChangeSet.size());
            }
        }
        return pendingChangeset;
    }

    private Database createTenantDatasource(TenantConfigDTO config, boolean resourcesOnFs) throws Exception {
        final String driver = DatabaseDriver.fromJdbcUrl(config.getDeDbUrl()).getDriverClassName();
        AbstractResourceAccessor resourceAccessor = getResourceAccessor(resourcesOnFs);
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

    private AbstractResourceAccessor getResourceAccessor(boolean resourcesOnFs) {
        if (resourcesOnFs) {
            log.debug("accessing master filename through filesystem");
            return new FileSystemResourceAccessor("/tmp/db/changelog/");
        }
        log.debug("accessing master filename through classpath");
        return new ClassLoaderResourceAccessor();
    }


}
