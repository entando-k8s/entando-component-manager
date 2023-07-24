package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.Setter;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("!test")
@Setter
public class TenantConfig {

    public String tenantConfigs;

    private ObjectMapper objectMapper;

    @Autowired
    public TenantConfig(
            @Value("${tenant-config}")
            String tenantConfigs,
            ObjectMapper objectMapper) {
        this.tenantConfigs = tenantConfigs;
        this.objectMapper = objectMapper;
    }

    @Bean
    public List<TenantConfigDTO> tenantConfigs() {
        List<TenantConfigDTO> tenantConfigList = null;
        if (tenantConfigs != null) {
            try {
                tenantConfigList = objectMapper.readValue(tenantConfigs, new TypeReference<List<TenantConfigDTO>>() {});
            } catch (final IOException e) {
                throw new EntandoComponentManagerException(e);
            }
        }
        return tenantConfigList;
    }

}
