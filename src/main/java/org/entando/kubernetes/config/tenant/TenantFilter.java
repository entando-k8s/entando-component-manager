package org.entando.kubernetes.config.tenant;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String HOST = "Host";
    private static final String REQUEST_SERVER_NAME = "Request server name";

    private final List<TenantConfigDTO> tenantConfigs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String headerXForwardedHost = request.getHeader(X_FORWARDED_HOST);
        String headerHost = request.getHeader(HOST);
        String headerServerName = request.getServerName();

        String tenantCode = this.fetchTenantCode(headerXForwardedHost, headerHost, headerServerName);

        TenantContextHolder.setCurrentTenantCode(tenantCode);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.destroy();
            log.info("Remove custom context from thread local.");
        }
    }

    public String fetchTenantCode(final String headerXForwardedHost,
                                  final String headerHost,
                                  final String servletRequestServerName) {

        log.debug("Extracting tenantCode from headerXForwardedHost:'{}' headerHost:'{}' servletRequestServerName:'{}'",
                headerXForwardedHost, headerHost, servletRequestServerName);
        String tenantCode = Optional.ofNullable(tenantConfigs)
                .flatMap(tcs ->
                        searchTenantCodeInConfigs(X_FORWARDED_HOST, headerXForwardedHost)
                                .or(() -> searchTenantCodeInConfigs(HOST, headerHost))
                                .or(() -> searchTenantCodeInConfigs(REQUEST_SERVER_NAME, servletRequestServerName)))
                .orElseGet(() -> {
                    log.info(
                            "No tenant identified for the received request. {}, {} and {} are empty. Falling back to {}",
                            X_FORWARDED_HOST, HOST, REQUEST_SERVER_NAME, EntandoMultiTenancy.PRIMARY_TENANT);
                    return EntandoMultiTenancy.PRIMARY_TENANT;
                });

        log.info("Extracted tenantCode: '{}'", tenantCode);
        return tenantCode;
    }

    private Optional<String> searchTenantCodeInConfigs(String searchInputName, String search) {

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
