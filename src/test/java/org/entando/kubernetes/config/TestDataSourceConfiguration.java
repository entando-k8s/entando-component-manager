package org.entando.kubernetes.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.entando.kubernetes.config.tenant.MultitenantDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@TestConfiguration
@Profile({"test", "testdb"})
public class TestDataSourceConfiguration {

    @Autowired(required = false)
    private TenantConfiguration tenantConfiguration;

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
        if (this.tenantConfiguration != null) {
            this.tenantConfiguration.tenantConfigs().stream()
                    .forEach(configuration -> {
                        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();

                        dataSourceBuilder.driverClassName(configuration.getDeDbDriverClassName());
                        dataSourceBuilder.username(configuration.getDeDbUsername());
                        dataSourceBuilder.password(configuration.getDeDbPassword());
                        dataSourceBuilder.url(configuration.getDeDbUrl());

                        String tenantCode = configuration.getTenantCode();

                        resolvedDataSources.put(tenantCode, dataSourceBuilder.build());
                    });
        }
        AbstractRoutingDataSource dataSource = new MultitenantDataSource();

        dataSource.setDefaultTargetDataSource(defaultDatasource);
        dataSource.setTargetDataSources(resolvedDataSources);
        dataSource.afterPropertiesSet();
        return dataSource;
    }


    public static final String JSONCONFIG = "[    {\n"
            + "        \"tenantCode\": \"tenant1\",\n"
            + "        \"fqdns\": \"tenant1.10-219-168-202.nip.io\",\n"
            + "        \"kcEnabled\": true,\n"
            + "        \"kcAuthUrl\": \"http://10-219-168-202.nip.io/auth\",\n"
            + "        \"kcRealm\": \"tenant1\",\n"
            + "        \"kcClientId\": \"quickstart\",\n"
            + "        \"kcClientSecret\": \"Pp67BmNXCyMyzDsVhnxRVUUdaCOnTJsR\",\n"
            + "        \"kcPublicClientId\": \"entando-web\",\n"
            + "        \"kcSecureUris\": \"\",\n"
            + "        \"kcDefaultAuthorizations\": \"\",\n"
            + "        \n"
            + "        \n"
            + "        \"dbDriverClassName\": \"org.postgresql.Driver\",\n"
            + "        \"dbUrl\": \"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.cluster.local:5432/tenant1\",\n"
            + "        \"dbUsername\": \"postgres\",\n"
            + "        \"dbPassword\": \"93b10c9a326445da\",\n"
            + "        \n"
            + "        \n"
            + "        \"deDbDriverClassName\": \"org.postgresql.Driver\",\n"
            + "        \"deDbUrl\": \"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.cluster.local:5432/"
            + "tenant1_cm?currentSchema=quickstart_dedb_65643\",\n"
            + "        \"deDbUsername\": \"postgres\",\n"
            + "        \"deDbPassword\": \"93b10c9a326445da\",\n"
            + "        \n"
            + "        \"deKcClientId\": \"quickstart-de\",\n"
            + "        \"deKcClientSecret\": \"kYSBoWl9RYblwEdbG17OMTWMnD8muCKa\",\n"
            + "        \n"
            + "        \"cdsPublicUrl\": \"http://cds.quickstart.10-219-168-202.nip.io/tenant1/\",\n"
            + "        \"cdsPrivateUrl\": \"http://quickstart-cds-tenant1-service:8080\",\n"
            + "        \"cdsPath\": \"api/v1\",\n"
            + "        \"solrAddress\": \"http://solr:8983/solr\",\n"
            + "        \"solrCore\": \"tenant1\"\n"
            + "    },{\n"
            + "        \"tenantCode\": \"tenant2\",\n"
            + "        \"fqdns\": \"tenant2.10-219-168-202.nip.io\",\n"
            + "        \"kcEnabled\": true,\n"
            + "        \"kcAuthUrl\": \"http://10-219-168-202.nip.io/auth\",\n"
            + "        \"kcRealm\": \"tenant2\",\n"
            + "        \"kcClientId\": \"quickstart\",\n"
            + "        \"kcClientSecret\": \"bKICzMCnNyMVM2fEbNvdI5X9hVSSe3qq\",\n"
            + "        \"kcPublicClientId\": \"entando-web\",\n"
            + "        \"kcSecureUris\": \"\",\n"
            + "        \"kcDefaultAuthorizations\": \"\",\n"
            + "        \"dbDriverClassName\": \"org.postgresql.Driver\",\n"
            + "        \"dbUrl\": \"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.cluster.local:5432/granatieri\",\n"
            + "        \"dbUsername\": \"postgres\",\n"
            + "        \"dbPassword\": \"93b10c9a326445da\",\n"
            + "        \n"
            + "        \"deDbDriverClassName\": \"org.postgresql.Driver\",\n"
            + "        \"deDbUrl\": \"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.cluster.local:5432/"
            + "tenant2_cm?currentSchema=quickstart_dedb_65643\",\n"
            + "        \"deDbUsername\": \"postgres\",\n"
            + "        \"deDbPassword\": \"93b10c9a326445da\",\n"
            + "        \n"
            + "        \"deKcClientId\": \"quickstart-de\",\n"
            + "        \"deKcClientSecret\": \"CRy2eikW2zyWidjxvY7Fn4yP69Gazt2R\",\n"
            + "        \n"
            + "        \"cdsPublicUrl\": \"http://cds.quickstart.10-219-168-202.nip.io/tenant2/\",\n"
            + "        \"cdsPrivateUrl\": \"http://quickstart-cds-tenant2-service:8080\",\n"
            + "        \"cdsPath\": \"api/v1\",\n"
            + "        \"solrAddress\": \"http://solr:8983/solr\",\n"
            + "        \"solrCore\": \"tenant2\"\n"
            + "    }\n"
            + "]";

}
