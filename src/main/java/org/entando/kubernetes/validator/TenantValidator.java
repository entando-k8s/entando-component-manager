package org.entando.kubernetes.validator;

import static org.entando.kubernetes.validator.ValidationFunctions.validateFQDN;
import static org.entando.kubernetes.validator.ValidationFunctions.validateURL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;

@Slf4j
public class TenantValidator {

    private Map<String, List<String>> validationErrors;

    private TenantValidator(List<TenantConfigDTO> tenants) {
        if (tenants != null && !tenants.isEmpty()) {

            // check domains
            tenants.forEach(config -> {
                // fqdns CSV
                validateFqdns(config.getFqdns(), config.getTenantCode());
                // KC auth URL
                if (!validateURL(config.getKcAuthUrl(), true, false, true)) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("kcAuthUrl: invalid URL detected '" + config.getKcAuthUrl() + "'");
                }
                // check for database values
                if (StringUtils.isBlank(config.getCmDbJdbcUrl())) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("cmDbJdbcUrl: missing configuration value");
                }
                if (StringUtils.isBlank(config.getCmDbPassword())) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("cmDbPassword: missing configuration value");
                }
                if (StringUtils.isBlank(config.getCmDbUsername())) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("cmDbUsername: missing configuration value");
                }
            });
            // check uniqueness of crucial settings
            validateTenantIdUniqueness(tenants);
            validateFqdnsUniqueness(tenants);
        }
    }

    private void validateTenantIdUniqueness(List<TenantConfigDTO> tenants) {
        final Map<String, Boolean> ids = new HashMap<>();

        if (tenants != null && !tenants.isEmpty()) {
            tenants.stream()
                    .filter(cfg -> {
                        boolean isDuplicated = ids.containsKey(cfg.getTenantCode());

                        if (isDuplicated) {
                            getErrorListForTenant(cfg.getTenantCode())
                                    .add("tenant with FQDNs' " + cfg.getFqdns() + "' is using the same tenant id (" + cfg.getTenantCode() + ")");
                        }
                        return !isDuplicated;
                    })
                    .forEach(cfg -> ids.put(cfg.getTenantCode(), true));
        }
    }

    private void validateFqdns(String fqdnsValueString, String tenantCode) {
        if (StringUtils.isNotBlank(fqdnsValueString)) {
            String[] fqdns = fqdnsValueString.split(",");
            Arrays.asList(fqdns).forEach(fqdn -> {
                if (!validateFQDN(fqdn)) {
                    getErrorListForTenant(tenantCode).add("fqdns: invalid value detected '" + fqdn + "'");
                }
            });
        }
    }

    private void validateFqdnsUniqueness(List<TenantConfigDTO> tenants) {
        if (tenants != null && !tenants.isEmpty()) {
            final Map<String, String> fqdns = new HashMap<>();

            tenants.forEach(config -> {
                final String tenantCode = config.getTenantCode();
                final String fqdnsStr = config.getFqdns();
                if (StringUtils.isNotBlank(fqdnsStr)) {
                    String[] fqdnsarr = fqdnsStr.split(",");

                    Arrays.asList(fqdnsarr).forEach(fqdn -> {
                        if (!fqdns.containsKey(fqdn)) {
                            fqdns.put(fqdn, tenantCode);
                        } else {
                            getErrorListForTenant(tenantCode).add("fqdns: '" + fqdn + "' already used for tenant '" + fqdns.get(fqdn) + "'");
                        }
                    });
                }
            });
        }
    }

    private List<String> getErrorListForTenant(String id) {
        if (validationErrors == null) {
            validationErrors = new HashMap<>();
        }
        validationErrors.computeIfAbsent(id, s -> {
            final List<String> tenantValidationErrors = new ArrayList<>();

            return tenantValidationErrors;
        });
        return validationErrors.get(id);
    }

    public Optional<Map<String, List<String>>> getValidationErrorMap() {
        if (validationErrors != null
                && !validationErrors.isEmpty()) {
            validationErrors.keySet().forEach(k -> {
                log.error("Tenant '{}'", k);
                validationErrors.get(k).forEach(e -> log.error("\t{}", e));
            });
        }
        return Optional.ofNullable(validationErrors);
    }

    public static TenantValidator validate(List<TenantConfigDTO> tenants) {
        return new TenantValidator(tenants);
    }

}
