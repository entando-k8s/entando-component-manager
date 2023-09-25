package org.entando.kubernetes.config.tenant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@AllArgsConstructor
public class TenantDataSourceConfiguration {

    private List<TenantConfigDTO> tenantConfigs;

    @PostConstruct
    public void applyMigrationsToTenants() throws LiquibaseException {
        new TenantLiquibaseMigration().migrate(tenantConfigs);
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
}
