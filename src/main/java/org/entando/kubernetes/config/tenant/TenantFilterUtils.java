package org.entando.kubernetes.config.tenant;

import static com.google.common.net.HttpHeaders.HOST;
import static com.google.common.net.HttpHeaders.X_FORWARDED_HOST;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;

@Slf4j
@UtilityClass
public class TenantFilterUtils {

    private static final String REQUEST_SERVER_NAME = "Request server name";

    public static String fetchTenantCode(final List<TenantConfigDTO> tenantConfigs,
                                         final String headerXEntandoTenantCode,
                                         final String headerXForwardedHost,
                                  final String headerHost,
                                  final String servletRequestServerName) {

        log.debug("Extracting tenantCode from headerXEntandoTenantCode:'{}' headerXForwardedHost:'{}' headerHost:'{}' servletRequestServerName:'{}'",
                headerXEntandoTenantCode, headerXForwardedHost, headerHost, servletRequestServerName);
        String tenantCode = Optional.ofNullable(headerXEntandoTenantCode)
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> fetchFromHeaderForClientToServer(tenantConfigs, headerXForwardedHost, headerHost, servletRequestServerName));

        log.info("Extracted tenantCode: '{}'", tenantCode);
        return tenantCode;
    }

    private static String fetchFromHeaderForClientToServer(final List<TenantConfigDTO> tenantConfigs,
                                                           final String headerXForwardedHost,
                                                           final String headerHost,
                                                           final String servletRequestServerName) {
        return Optional.ofNullable(tenantConfigs)
                .flatMap(tcs ->
                        searchTenantCodeInConfigs(tenantConfigs, X_FORWARDED_HOST, headerXForwardedHost)
                                .or(() -> searchTenantCodeInConfigs(tenantConfigs, HOST, headerHost))
                                .or(() -> searchTenantCodeInConfigs(tenantConfigs, REQUEST_SERVER_NAME, servletRequestServerName)))
                .orElseGet(() -> {
                    log.info(
                            "No tenant identified for the received request. {}, {} and {} are empty. Falling back to {}",
                            X_FORWARDED_HOST, HOST, REQUEST_SERVER_NAME, EntandoMultiTenancy.PRIMARY_TENANT);
                    return EntandoMultiTenancy.PRIMARY_TENANT;
                });
    }

    private static Optional<String> searchTenantCodeInConfigs(final List<TenantConfigDTO> tenantConfigs,
                                                              String searchInputName,
                                                              String search) {

        if (StringUtils.isBlank(search)) {
            return Optional.empty();
        }

        return tenantConfigs.stream().filter(t -> getFqdnTenantNames(t).contains(search)).findFirst()
                .map(TenantConfigDTO::getTenantCode)
                .or(() -> {
                    log.info(
                            "No tenant identified for the received request. {} = '{}'. Falling back to {}",
                            searchInputName, search, EntandoMultiTenancy.PRIMARY_TENANT);
                    return Optional.of(EntandoMultiTenancy.PRIMARY_TENANT);
                });
    }

    private List<String> getFqdnTenantNames(TenantConfigDTO tenant) {
        String[] fqdns = tenant.getFqdns().replaceAll("\\s", "").split(",");
        return Arrays.asList(fqdns);
    }
}
