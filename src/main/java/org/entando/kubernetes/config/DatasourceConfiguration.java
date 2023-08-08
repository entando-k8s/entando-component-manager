/*
 * Copyright 2023-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.entando.kubernetes.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.entando.kubernetes.config.tenant.MultitenantDataSource;
import org.entando.kubernetes.config.tenant.TenantConfigurationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Configuration
@Profile({"!test", "!testdb"})
@DependsOn("tenantConfigs")
public class DatasourceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceConfiguration.class);

    @Autowired(required = false)
    private List<TenantConfigurationDTO> tenantConfigs;

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties) {
        Map<Object, Object> resolvedDataSources = new HashMap<>();
        HikariDataSource defaultDatasource = properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        if (this.tenantConfigs != null
                && !tenantConfigs.isEmpty()) {
            this.tenantConfigs.stream()
                    .forEach(configuration -> {
                        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();

                        dataSourceBuilder.driverClassName(configuration.getDeDbDriverClassName());
                        dataSourceBuilder.username(configuration.getDeDbUsername());
                        dataSourceBuilder.password(configuration.getDeDbPassword());
                        dataSourceBuilder.url(configuration.getDeDbUrl());

                        String tenantCode = configuration.getTenantCode();
                        resolvedDataSources.put(tenantCode, dataSourceBuilder.build());
                        logger.info("Imported database settings for {}", tenantCode);
                    });
        }
        AbstractRoutingDataSource dataSource = new MultitenantDataSource();

        dataSource.setDefaultTargetDataSource(defaultDatasource);
        dataSource.setTargetDataSources(resolvedDataSources);
        dataSource.afterPropertiesSet();
        return dataSource;
    }

}
