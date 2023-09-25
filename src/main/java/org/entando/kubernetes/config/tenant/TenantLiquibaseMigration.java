package org.entando.kubernetes.config.tenant;


import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;
import org.springframework.boot.jdbc.DatabaseDriver;

public class TenantLiquibaseMigration {

    public void migrate(List<TenantConfigDTO> tenantConfigs) throws LiquibaseException {

        for (TenantConfigDTO config : tenantConfigs) {

            if (config instanceof PrimaryTenantConfig) {
                continue;
            }

            Database database = createTenantDatasource(config);
            Liquibase liquibase = new Liquibase("classpath:db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts("standard"), new LabelExpression());
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
}
