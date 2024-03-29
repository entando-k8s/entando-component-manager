package org.entando.kubernetes.config.tenant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TenantDataSourceConfiguration {

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
