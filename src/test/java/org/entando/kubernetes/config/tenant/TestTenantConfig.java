package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.security.MultipleIdps;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestTenantConfig {

    @Value("${entando.tenants:#{null}}")
    private String tenantConfigs;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public List<TenantConfigDTO> tenantConfigs() {

        List<TenantConfigDTO> tenantConfigList = new ArrayList<>();

        if (StringUtils.isNotBlank(tenantConfigs)) {
            try {
                tenantConfigList = objectMapper.readValue(tenantConfigs, new TypeReference<List<TenantConfigDTO>>() {
                });
            } catch (final IOException e) {
                throw new EntandoComponentManagerException(e);
            }
        }

        return tenantConfigList;
    }

    @Bean
    public MultipleIdps multipleIdps(List<TenantConfigDTO> tenantConfigs) {
        return new MultipleIdps(tenantConfigs);
    }
}