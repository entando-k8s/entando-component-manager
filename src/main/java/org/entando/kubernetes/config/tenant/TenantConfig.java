package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.security.MultipleIdps;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("!test")
@Slf4j
public class TenantConfig {

    private String tenantConfigs;

    private ObjectMapper objectMapper;

    @Autowired
    public TenantConfig(
            @Value("${entando.tenants:#{null}}")
            String tenantConfigs,
            ObjectMapper objectMapper) {
        this.tenantConfigs = tenantConfigs;
        this.objectMapper = objectMapper;
    }

    @Bean
    public List<TenantConfigDTO> tenantConfigs(PrimaryTenantConfig primaryTenantConfig) {

        List<TenantConfigDTO> tenantConfigList = new ArrayList<>();

        if (StringUtils.isNotBlank(tenantConfigs)) {
            try {
                tenantConfigList = objectMapper.readValue(tenantConfigs, new TypeReference<List<TenantConfigDTO>>() {
                });
                log.info("Tenant configurations have been parsed successfully");
            } catch (final IOException e) {
                throw new EntandoComponentManagerException(e);
            }
        }

        // add primary
        tenantConfigList.add(primaryTenantConfig);

        return tenantConfigList;
    }

    @Bean
    public PrimaryTenantConfig primaryTenantConfig(
            @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") final String primaryIssuerUri,
            @Value("${entando.app.host.name}") final String primaryHostName,
            @Value("${spring.jpa.database-platform}") final String primaryDbDialect,
            @Value("${spring.datasource.username}") final String primaryDbUsername,
            @Value("${spring.datasource.password}") final String primaryDbPassword,
            @Value("${spring.datasource.url}") final String primaryDbUrl) {

        return new PrimaryTenantConfig()
                .setTenantCode(EntandoMultiTenancy.PRIMARY_TENANT)
                .setFqdns(primaryHostName)
                .setKcAuthUrl(primaryIssuerUri)
                .setDeDbDriverClassName(primaryDbDialect)
                .setDeDbUrl(primaryDbUrl)
                .setDeDbUsername(primaryDbUsername)
                .setDeDbPassword(primaryDbUsername);
    }

    @Bean
    public MultipleIdps multipleIdps(List<TenantConfigDTO> tenantConfigs) {
        return new MultipleIdps(tenantConfigs);
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    public static class PrimaryTenantConfig extends TenantConfigDTO {

        private String tenantCode;
        private String fqdns;
        private String kcAuthUrl;
        private String deDbDriverClassName;
        private String deDbUrl;
        private String deDbUsername;
        private String deDbPassword;
    }
}
