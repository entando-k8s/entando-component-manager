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
                if (StringUtils.isBlank(config.getDeDbUrl())) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("deDbUrl: missing configuration value");
                }
                if (StringUtils.isBlank(config.getDeDbPassword())) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("deDbPassword: missing configuration value");
                }
                if (StringUtils.isBlank(config.getDeDbUsername())) {
                    getErrorListForTenant(config.getTenantCode())
                            .add("deDbUsername: missing configuration value");
                }
            });
            // check uniqueness of crucial settings
            validateTenantIdUniqueness(tenants);
            validateFqdnsUniqueness(tenants);
        }
    }

    private void validateTenantIdUniqueness(List<TenantConfigDTO> tenants) {
        final Map<String, Boolean> ids = new HashMap<>();

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
                        getErrorListForTenant(tenantCode).add("fqdns: '" + fqdn + "' already used by tenant '" + fqdns.get(fqdn) + "'");
                    }
                });
            }
        });
    }

    private List<String> getErrorListForTenant(String id) {
        if (validationErrors == null) {
            validationErrors = new HashMap<>();
        }
        validationErrors.computeIfAbsent(id, key -> new ArrayList<>());
        return validationErrors.get(id);
    }

    public Optional<Map<String, List<String>>> getValidationErrorMap() {
        if (validationErrors != null
                && !validationErrors.isEmpty()) {
            final StringBuilder logline = new StringBuilder();

            logline.append("Tenant configuration error detected! See details below:\n");
            validationErrors.keySet().forEach(k -> {
                logline.append("Tenant '").append(k).append("'\n");
                validationErrors.get(k).forEach(e -> logline.append("\t").append(e).append("\n"));
            });
            log.error(logline.toString());
        }
        return Optional.ofNullable(validationErrors);
    }

    public static TenantValidator validate(List<TenantConfigDTO> tenants) {
        return new TenantValidator(tenants);
    }

}
