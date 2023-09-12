package org.entando.kubernetes.service.update;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UpdateDatabase implements IUpdateDatabase {

    //    public static final String CHANGELOG_MASTER_YAML = "classpath:db/changelog/db.changelog-master.yaml";
    public static final String CHANGELOG_MASTER_YAML = "db.changelog-master.yaml";

    private final DataSource dataSource;

    private final List<TenantConfigDTO> tenantConfigs;

    private final ResourceLoader resourceLoader;

    private Resource changelog;


    public UpdateDatabase(DataSource dataSource, List<TenantConfigDTO> tenantConfigs, ResourceLoader resourceLoader) {
        try {
            this.changelog = resourceLoader.getResource("classpath:db/changelog/db.changelog-master.yaml");

            if (changelog.exists()) {
                System.out.println("\n\n*******\n*******\n*******\n*******\n*******\n");
                System.out.println(" TROVATO " + changelog.getURI());
                System.out.println(" TROVATO " + changelog.getURL());
                System.out.println(" TROVATO " + changelog.getFile().getAbsolutePath());
                System.out.println(" ESISTENTE: " + dataSource.getConnection().getMetaData().getURL());
                tenantConfigs.forEach(cfg -> System.out.println(" " + cfg.getDeDbUrl()));
                System.out.println("*******\n*******\n*******\n*******\n*******\n");
            } else {
                System.out.println("ERRROE");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.dataSource = dataSource;
        this.tenantConfigs = tenantConfigs;
        this.resourceLoader = resourceLoader;
    }


    @PostConstruct
    public void testme() {
        System.out.println("\n\n*******\n*******");
        try {
            checkForDbSchemaUpdate(changelog.getFile().getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("*******\n*******");
    }


    public Database createTenantDatasource(TenantConfigDTO config) throws DatabaseException {
        final String driver = DatabaseDriver.fromJdbcUrl(config.getDeDbUrl()).getDriverClassName();

        System.out.println("\ncreateTenantDatasource\n deDBUrl " + config.getDeDbUrl());
        System.out.println("\ncreateTenantDatasource\n driver " + driver);

        ResourceAccessor resourceAccessor = new FileSystemResourceAccessor();

        Database database = DatabaseFactory.getInstance().openDatabase(
                config.getDeDbUrl(),
                config.getDeDbUsername(),
                config.getDeDbPassword(),
                driver,
                null, null, null, resourceAccessor);

        database.setDatabaseChangeLogTableName("liquibasechangelog");
        database.setDatabaseChangeLogLockTableName("liquibasechangeloglock");

        return database;
        //
        //        return DataSourceBuilder
        //                .create()
        //                .url(config.getDeDbUrl())
        //                .username(config.getDeDbUsername())
        //                .password(config.getDeDbPassword())
        //                .driverClassName(driver)
        //                .build();

    }

    private boolean checkForDbSchemaUpdate(String masterFilePath) {
        final ResourceAccessor resourceAccessor = new FileSystemResourceAccessor();

        try {
            final String actualJdbcUrl = dataSource.getConnection().getMetaData().getURL();
            tenantConfigs
                    .stream()
                    .peek(cfg -> log.debug("actual jdbcUrl {} {}", cfg.getDeDbUrl(), actualJdbcUrl))
                    .filter(cfg -> !cfg.getDeDbUrl().contains(actualJdbcUrl))
                    .forEach(cfg -> {
                        updateTenantDatabase_alt(cfg, masterFilePath);
                    });
        } catch (SQLException e) {
            log.error("error updating database", e);
        }
        return false;
    }


    private Liquibase createLiquibaseFromTenantDefinition(TenantConfigDTO tenantConfig, String masterFilePath)
            throws DatabaseException, IOException {
        Liquibase liquibase = null;
        Database database = createTenantDatasource(tenantConfig);

//        return new Liquibase("db.changelog-master.yaml",
//                new FileSystemResourceAccessor("/home/matteo/lavoro/progetti/entando/entando-component-manager/target/classes/db/changelog"), database);
        String changeLogFilePath =  changelog.getFile().getAbsolutePath();
        String changelogPath = changeLogFilePath.substring(0, changeLogFilePath.lastIndexOf('/'));
        log.debug("Path of the master changelog {} ", changelogPath);
        return new Liquibase("db.changelog-master.yaml", new FileSystemResourceAccessor(changelogPath), database);

    }


    @Override
    public void updateTenantDatabase(TenantConfigDTO tenantConfig, String masterFilePath) {
        try {
            Database database = createTenantDatasource(tenantConfig);
//            Liquibase liquibase = new Liquibase("db.changelog-master.yaml",
//                    new FileSystemResourceAccessor("/home/matteo/lavoro/progetti/entando/entando-component-manager/target/classes/db/changelog"), database);

            Liquibase liquibase = createLiquibaseFromTenantDefinition(tenantConfig, masterFilePath);
            if (liquibase.listUnrunChangeSets(null, null).size() > 0) {
                System.out.println("§§§§§ " + liquibase.listUnrunChangeSets(null, null).size() + " UPDATE AVAILABLE §§§§");
//                  liquibase.update("");
            } else {
                System.out.println("§§§§§ UPDATE NOT AVAILABLE §§§§");
            }
            System.out.println("@@@ " + liquibase.getDatabase().getShortName());

        } catch (Exception e) {
            log.error("error checking for schema update");
            e.printStackTrace();
        }
    }


    protected DataSource createDataSource(TenantConfigDTO tenant) {
        final String driver = DatabaseDriver.fromJdbcUrl(tenant.getDeDbUrl()).getDriverClassName();

        return DataSourceBuilder
                .create()
                .url(tenant.getDeDbUrl()) // Replace with your database URL
                .username(tenant.getDeDbUsername()) // Replace with your database username
                .password(tenant.getDeDbPassword()) // Replace with your database password
                .driverClassName(driver) // Replace with the appropriate driver class
                .build();
    }


    public void updateTenantDatabase_alt(TenantConfigDTO tenantConfig, String changelogFilePath) {
        Map<String, Object> config = new HashMap<>();
        config.put("liquibase.licenseKey", "YOUR_PRO_KEY");

        try {
            Scope.child(config, () -> {
                Connection connection = createDataSource(tenantConfig).getConnection();

                // ResourceAccessor accessor= new FileSystemResourceAccessor("/home/matteo/lavoro/progetti/entando/
                // entando-component-manager/target/test-classes/db/changelog");
                // ClassLoaderResourceAccessor accessor = new ClassLoaderResourceAccessor();

                ClassLoader classLoader = EntandoKubernetesJavaApplication.class.getClassLoader();
                ClassLoaderResourceAccessor accessor = new ClassLoaderResourceAccessor(classLoader);

                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

                Liquibase liquibase = new Liquibase("classpath:db/changelog/changelog-master.yaml", accessor, database);

                //Liquibase calls will go here
                System.out.println("@@@@ " + liquibase.getDatabase().getShortName());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
