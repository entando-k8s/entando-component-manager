package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
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
public class TenantConfiguration {

    private String tenantConfigs;

    private ObjectMapper objectMapper;

    @Autowired
    public TenantConfiguration(
            @Value("${ENTANDO_TENANTS:#{null}}")
            String tenantConfigs,
            ObjectMapper objectMapper) {
        this.tenantConfigs = tenantConfigs;
        this.objectMapper = objectMapper;
    }

    private boolean isValidDeDbConfiguration(TenantConfigurationDTO config) {
        boolean isOk = (StringUtils.isNotBlank(config.getDeDbDriverClassName())
                && StringUtils.isNotBlank(config.getDeDbUsername())
                && StringUtils.isNotBlank(config.getDeDbPassword())
                && StringUtils.isNotBlank(config.getDeDbUrl()));
        if (!isOk) {
            log.warn("Invalid DE database configuration detected for tenant '{}'",
                    config.getTenantCode());
        }
        return isOk;
    }

    @Bean
    public List<TenantConfigurationDTO> tenantConfigs() {
        List<TenantConfigurationDTO> tenantConfigList = null;
        if (StringUtils.isNotBlank(tenantConfigs)) {
            try {
                tenantConfigList = objectMapper.readValue(tenantConfigs, new TypeReference<List<TenantConfigurationDTO>>() {});
                log.info("Tenant configurations have been parsed successfully");
                List<TenantConfigurationDTO> configurationErrors = tenantConfigList.stream()
                        .filter(cfg -> !isValidDeDbConfiguration(cfg))
                        .collect(Collectors.toList());
                if (configurationErrors != null
                        && !configurationErrors.isEmpty()) {
                    throw new EntandoComponentManagerException("tenant configuration error detected");
                }
            } catch (final IOException e) {
                throw new EntandoComponentManagerException(e);
            }
        }
        return tenantConfigList;
    }

}
