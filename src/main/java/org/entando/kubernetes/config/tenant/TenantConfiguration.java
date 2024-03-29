package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.security.MultipleIdps;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.entando.kubernetes.validator.TenantValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Profile.html
// the profile are by default in OR we need AND
//@Profile("!test  & !testdb")
@Slf4j
public class TenantConfiguration {

    @Bean
    public List<TenantConfigDTO> tenantConfigs(ObjectMapper objectMapper,
                                               @Value("${entando.tenants:#{null}}") String tenantConfigs,
                                               @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") final String primaryIssuerUri,
                                               @Value("${spring.security.oauth2.client.registration.oidc.client-id}") final String clientId,
                                               @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") final String clientSecret,
                                               @Value("${entando.app.host.name:localhost}") final String primaryHostName,
                                               @Value("${spring.datasource.username}") final String primaryDbUsername,
                                               @Value("${spring.datasource.password}") final String primaryDbPassword,
                                               @Value("${spring.datasource.url}") final String primaryDbUrl) {
        log.debug("==== starting Tenant Configs ====");
        List<TenantConfigDTO> tenantConfigList = new ArrayList<>();

        if (StringUtils.isNotBlank(tenantConfigs)) {
            try {
                tenantConfigList = objectMapper.readValue(tenantConfigs, new TypeReference<>() {
                });
                log.info("Tenant configurations have been parsed successfully");
            } catch (final IOException e) {
                throw new EntandoComponentManagerException(e);
            }
            // validate config
            if (TenantValidator.validate(tenantConfigList).getValidationErrorMap().isPresent()) {
                throw new EntandoComponentManagerException("Tenant validation configuration errors detected");
            }
        }

        // add primary
        tenantConfigList.add(new PrimaryTenantConfig()
                .setTenantCode(EntandoMultiTenancy.PRIMARY_TENANT)
                .setKcRealm("entando")
                .setFqdns(primaryHostName)
                .setKcAuthUrl(primaryIssuerUri)
                .setDeKcClientId(clientId)
                .setDeKcClientSecret(clientSecret)
                .setDeDbUrl(primaryDbUrl)
                .setDeDbUsername(primaryDbUsername)
                .setDeDbPassword(primaryDbPassword));

        return tenantConfigList;
    }

    @Bean
    public MultipleIdps multipleIdps(@Qualifier("tenantConfigs") List<TenantConfigDTO> tenantConfigs) {
        log.debug("==== starting MultipleIdps configs ====");
        return new MultipleIdps(tenantConfigs);
    }

    @Setter
    @Getter
    @EqualsAndHashCode
    @ToString(exclude = {"deDbPassword"})
    @Accessors(chain = true)
    public static class PrimaryTenantConfig extends TenantConfigDTO {

        private String tenantCode;
        private String fqdns;
        private String kcAuthUrl;
        private String kcRealm;
        private String deDbUrl;
        private String deDbUsername;
        private String deDbPassword;
        private String deKcClientId;
        private String deKcClientSecret;
    }
}
