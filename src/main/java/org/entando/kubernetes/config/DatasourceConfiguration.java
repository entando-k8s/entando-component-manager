/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.entando.kubernetes.config;

import com.zaxxer.hikari.HikariDataSource;
import org.entando.kubernetes.config.tenant.MultitenantDataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.entando.kubernetes.config.tenant.TenantConfig;
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
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Configuration
@DependsOn("tenantConfiguration")
public class DatasourceConfiguration {
    
    private static final String DB_DE_DRIVER_CLASS_NAME_PROPERTY = "deDbDriverClassName";
    private static final String DB_DE_URL_PROPERTY = "deDbUrl";
    private static final String DB_DE_USERNAME_PROPERTY = "deDbUsername";
    private static final String DB_DE_PASSWORD_PROPERTY = "deDbPassword";
    private static final String DB_DE_MAX_TOTAL_PROPERTY = "deDbMaxTotal";
    private static final String DB_DE_MAX_IDLE_PROPERTY = "deDbMaxIdle";
    private static final String DB_DE_MAX_WAIT_MS_PROPERTY = "deDbMaxWaitMillis";
    private static final String DB_DE_INITIAL_SIZE_PROPERTY = "deDbInitialSize";
    
    private static final Logger logger = LoggerFactory.getLogger(DatasourceConfiguration.class);
    
    @Autowired(required = false)
    private Map<String, TenantConfig> tenantConfigs;
    
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties) {
        HikariDataSource defaultDatasource = properties
                .initializeDataSourceBuilder().type(HikariDataSource.class).build();
        Map<Object, Object> resolvedDataSources = new HashMap<>();
        if (null != this.tenantConfigs) {
            for (TenantConfig tenantConfig : this.tenantConfigs.values()) {
                DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
                String tenantCode = tenantConfig.getTenantCode();
                dataSourceBuilder.driverClassName(tenantConfig.getProperty(DB_DE_DRIVER_CLASS_NAME_PROPERTY).orElseThrow());
                dataSourceBuilder.username(tenantConfig.getProperty(DB_DE_USERNAME_PROPERTY).orElseThrow());
                dataSourceBuilder.password(tenantConfig.getProperty(DB_DE_PASSWORD_PROPERTY).orElseThrow());
                dataSourceBuilder.url(tenantConfig.getProperty(DB_DE_URL_PROPERTY).orElseThrow());
                resolvedDataSources.put(tenantCode, dataSourceBuilder.build());
                logger.debug("Created Datasource for tenant {}", tenantCode);
            }
        }
        AbstractRoutingDataSource dataSource = new MultitenantDataSource();
        dataSource.setDefaultTargetDataSource(defaultDatasource);
        dataSource.setTargetDataSources(resolvedDataSources);
        dataSource.afterPropertiesSet();
        logger.debug("Created MultitenantDatasource");
        return dataSource;
    }
    
}
