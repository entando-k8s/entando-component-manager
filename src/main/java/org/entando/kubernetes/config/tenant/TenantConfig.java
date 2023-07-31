package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
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
            @Value("${tenant-config:#{null}}")
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

}
