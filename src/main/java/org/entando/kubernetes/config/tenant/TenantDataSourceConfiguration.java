package org.entando.kubernetes.config.tenant;

import static org.entando.kubernetes.liquibase.helper.DbMigrationUtils.generateSecureRandomHash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@Slf4j
@Configuration
@AllArgsConstructor
public class TenantDataSourceConfiguration {

    public static final String DB_RESOURCES_SEARCH_PARAM = "classpath:db/**/*.yaml";
    public static final String SRC_DB_DIR = "db";

    private List<TenantConfigDTO> tenantConfigs;

    @PostConstruct
    public void applyMigrationsToTenants() throws Exception {
        log.debug("checking for DB update");
        String dbTmpDir = copyLiquibaseResources();
        new TenantLiquibaseMigration().migrate(tenantConfigs, dbTmpDir);
        log.debug("DB update check completed");
    }

    @Bean
    public DataSource dataSource(@Qualifier("tenantConfigs") List<TenantConfigDTO> tenantConfigs) {
        log.debug("==== starting DB routing ====");
        Map<Object, Object> resolvedDataSources =  tenantConfigs.stream()
                .collect(Collectors.toMap(TenantConfigDTO::getTenantCode, this::buildDataSourceFromTenantConfiguration));

        TenantDataSource dataSource = new TenantDataSource();
        dataSource.setDefaultTargetDataSource(resolvedDataSources.get(EntandoMultiTenancy.PRIMARY_TENANT));
        dataSource.setTargetDataSources(resolvedDataSources);
        dataSource.afterPropertiesSet();

        return dataSource;
    }

    private DataSource buildDataSourceFromTenantConfiguration(TenantConfigDTO config) {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

        dataSourceBuilder.driverClassName(DatabaseDriver.fromJdbcUrl(config.getDeDbUrl()).getDriverClassName());
        dataSourceBuilder.username(config.getDeDbUsername());
        dataSourceBuilder.password(config.getDeDbPassword());
        dataSourceBuilder.url(config.getDeDbUrl());
        log.info("Built database settings for {}", config.getTenantCode());

        return dataSourceBuilder.build();
    }

    private String copyLiquibaseResources() throws IOException {
        final String tmpFolder = System.getProperty("java.io.tmpdir");
        final String dbDirectoryName = SRC_DB_DIR
                + '-'
                + generateSecureRandomHash(6);

        log.debug("==== starting DB changelog copy into filesystem ====");
        ResourcePatternResolver resourcePatResolver = new PathMatchingResourcePatternResolver();
        Resource[] allResources = resourcePatResolver.getResources(DB_RESOURCES_SEARCH_PARAM);


        if (allResources.length != 0) {
            for (Resource resource: allResources) {
                String uri = resource.getURI().toString();
                uri = uri.substring(uri.lastIndexOf(File.separator + SRC_DB_DIR + File.separator));
                uri = uri.replace(SRC_DB_DIR, dbDirectoryName);
                Path destinationFile = Path.of(tmpFolder, uri);

                FileUtils.copyInputStreamToFile(resource.getInputStream(), destinationFile.toFile());
                log.debug("Moving Liquibase resources from JAR to {}", destinationFile.toFile().getAbsolutePath());
            }
        } else {
            final String resourcesPath = "src/main/resources/db";
            final File destDir = new File(tmpFolder + File.separator + dbDirectoryName);
            final File dbDirectory = new File(resourcesPath);

            log.debug("Moving Liquibase resources to {}", destDir.getAbsolutePath());
            FileUtils.copyDirectory(dbDirectory, destDir);
        }
        return dbDirectoryName;
    }

}
