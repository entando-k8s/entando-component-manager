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
import org.entando.kubernetes.exception.EntandoComponentManagerException;
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
    public List<TenantConfigDTO> tenantConfigs() {
        List<TenantConfigDTO> tenantConfigList = null;
        if (StringUtils.isNotBlank(tenantConfigs)) {
            try {
                tenantConfigList = objectMapper.readValue(tenantConfigs, new TypeReference<List<TenantConfigDTO>>() {});
                log.info("Tenant configurations have been parsed successfully");
            } catch (final IOException e) {
                throw new EntandoComponentManagerException(e);
            }
        }
        return tenantConfigList;
    }

    @Bean
    public MultipleIdps multipleIdps(
            @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuerUri,
            List<TenantConfigDTO> tenantConfigs) {

        KcTenantConfig primaryConfig = new KcTenantConfig()
                .setKcAuthUrl(issuerUri)
                .setTenantCode("primary"); // FIXME use custom model constant

        List<TenantConfigDTO> fullTenantConfigs = new ArrayList<>(tenantConfigs);
        fullTenantConfigs.add(primaryConfig);

        return new MultipleIdps(fullTenantConfigs);
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    private static class KcTenantConfig extends TenantConfigDTO {

        private String tenantCode;
        private String kcAuthUrl;
    }
}
