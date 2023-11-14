package org.entando.kubernetes.config.tenant;

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
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@Slf4j
@Configuration
@AllArgsConstructor
public class TenantDataSourceConfiguration {

    public static final String DB_RESOURCES_SEARCH_PARAM = "classpath:db/**/*.yaml";
    public static final String DB_CHANGELOG_MASTER_FILE = "classpath:db/changelog/db.changelog-master.yaml";


    private ResourceLoader resourceLoader;

    private List<TenantConfigDTO> tenantConfigs;

    @PostConstruct
    public void applyMigrationsToTenants() throws Exception {
        log.debug("checking for DB update");
        final boolean resourcesOnTmp = copyLiquibaseResources(resourceLoader);
        new TenantLiquibaseMigration().migrate(tenantConfigs, resourcesOnTmp);
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

    private boolean copyLiquibaseResources(@Qualifier("resourceLoader") ResourceLoader resourceLoader) {
        File changelog;
        boolean resourcesOnJar = false;

        log.debug("==== starting DB changelog copy into filesystem ====");
        try {
            ResourcePatternResolver resourcePatResolver = new PathMatchingResourcePatternResolver();
            Resource[] allResources = resourcePatResolver.getResources(DB_RESOURCES_SEARCH_PARAM);

            if (allResources.length != 0) {
                // inside a JAR!
                resourcesOnJar = true;
                for (Resource resource: allResources) {
                    String uri = resource.getURI().toString();
                    uri = uri.substring(uri.lastIndexOf("/db/"));
                    String tmpFolder = System.getProperty("java.io.tmpdir");
                    Path destinationFile = Path.of(tmpFolder, uri);

                    FileUtils.copyInputStreamToFile(resource.getInputStream(), destinationFile.toFile());
                    log.debug("Moving Liquibase file from JAR to {}", destinationFile.toFile().getAbsolutePath());
                }
            }
        } catch (IOException e) {
            log.error("Error copying Liquibase resources", e);
        }
        return resourcesOnJar;
    }

}
