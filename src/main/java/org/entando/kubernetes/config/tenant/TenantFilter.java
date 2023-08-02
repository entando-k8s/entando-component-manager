package org.entando.kubernetes.config.tenant;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    private static final String PRIMARY_TENANT_CODE = "primary";

    private final TenantConfiguration tenantConfiguration;

    public TenantFilter(TenantConfiguration tenantConfiguration) {
        this.tenantConfiguration = tenantConfiguration;
    }

    public String getTenantCode(final String headerXForwardedHost,
            final String headerHost,
            final String servletRequestServerName) {
        String tenantCode;
        if (tenantConfiguration == null) {
            tenantCode = PRIMARY_TENANT_CODE;
        } else {
            Optional<String> headerXForwarderHostTenantCode;
            Optional<String> headerXHostTenantCode;
            if (headerXForwardedHost != null && !headerXForwardedHost.isBlank()) {
                headerXForwarderHostTenantCode = getTenantCodeFromConfig(headerXForwardedHost);

                tenantCode = headerXForwarderHostTenantCode.orElse(PRIMARY_TENANT_CODE);
            } else if (headerHost != null && !headerHost.isBlank()) {
                headerXHostTenantCode = getTenantCodeFromConfig(headerHost);

                tenantCode = headerXHostTenantCode.orElse(PRIMARY_TENANT_CODE);

            } else if (servletRequestServerName != null && !servletRequestServerName.isBlank()) {
                Optional<String> servletNameTenantCode = getTenantCodeFromConfig(servletRequestServerName);
                tenantCode = servletNameTenantCode.orElse(PRIMARY_TENANT_CODE);
            } else {
                tenantCode = PRIMARY_TENANT_CODE;
            }
        }
        logger.info("TenantCode: " + tenantCode);
        return tenantCode;
    }

    private Optional<String> getTenantCodeFromConfig(String search) {
        Optional<TenantConfigurationDTO> tenant = tenantConfiguration.tenantConfigs().stream().filter(
                t -> getFqdnTenantNames(t).contains(search)).findFirst();
        return tenant.map(TenantConfigurationDTO::getTenantCode);
    }

    private List<String> getFqdnTenantNames(TenantConfigurationDTO tenant) {
        String[] fqdns = tenant.getFqdns().replaceAll("\\s", "").split(",");
        return Arrays.asList(fqdns);
    }
}
