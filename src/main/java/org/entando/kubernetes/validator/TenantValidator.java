package org.entando.kubernetes.validator;

import static org.entando.kubernetes.validator.ValidationFunctions.validateFQDN;
import static org.entando.kubernetes.validator.ValidationFunctions.validateURL;

import java.util.*;

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
                                .add("tenant with the couple FQDNs: '" + cfg.getFqdns() + "' - context: '" + cfg.getContext() + "' is using the same tenant id (" + cfg.getTenantCode() + ")");
                    }
                    return !isDuplicated;
                })
                .forEach(cfg -> ids.put(cfg.getTenantCode(), true));
    }

    private void validateFqdns(String fqdnsValueString, String tenantCode) {
        if (StringUtils.isNotBlank(fqdnsValueString)) {
            String[] fqdns = fqdnsValueString.split(",");
            Arrays.asList(fqdns).forEach(fqdn -> {
                if (!Objects.equals(fqdn, "localhost") && !validateFQDN(fqdn)) {
                    getErrorListForTenant(tenantCode).add("fqdns: invalid value detected '" + fqdn + "'");
                }
            });
        }
    }

    private void validateFqdnsUniqueness(List<TenantConfigDTO> tenants) {
        final Map<String, String> tenantsMap = new HashMap<>();

        tenants.forEach(config -> {
            final String tenantCode = config.getTenantCode();
            final String fqdnsStr = config.getFqdns();
            final String context = config.getContext();
            if (StringUtils.isNotBlank(fqdnsStr)) {
                String[] fqdnsArray = fqdnsStr.split(",");

                Arrays.asList(fqdnsArray).forEach(fqdn -> {
                    String tenantKey = fqdn + (StringUtils.isBlank(context) ? "" : ("|" + context));
                    if (!tenantsMap.containsKey(tenantKey)) {
                        tenantsMap.put(tenantKey, tenantCode);
                    } else {
                        getErrorListForTenant(tenantCode).add("The couple fqdns: '" + fqdn + "' - context: '" + context + "' already used by tenant '" + tenantsMap.get(tenantKey) + "'");
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
