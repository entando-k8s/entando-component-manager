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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String HOST = "Host";
    private static final String REQUEST_SERVER_NAME = "Request server name";
    private static final String PRIMARY_TENANT_CODE = "primary"; // FIXME use the custom model constant

    private final List<TenantConfigDTO> tenantConfigs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String headerXForwardedHost = request.getHeader(X_FORWARDED_HOST);
        String headerHost = request.getHeader(HOST);
        String headerServerName = request.getServerName();

        String tenantCode = this.getTenantCode(headerXForwardedHost, headerHost, headerServerName);

        TenantContextHolder.setCurrentTenantCode(tenantCode);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.destroy();
            log.info("Remove custom context from thread local.");
        }
    }

    public String getTenantCode(final String headerXForwardedHost,
            final String headerHost,
            final String servletRequestServerName) {

        String tenantCode = Optional.ofNullable(tenantConfigs)
                .flatMap(tcs ->
                        getTenantCodeFromConfig(X_FORWARDED_HOST, headerXForwardedHost)
                                .or(() -> getTenantCodeFromConfig(HOST, headerHost))
                                .or(() -> getTenantCodeFromConfig(REQUEST_SERVER_NAME, servletRequestServerName)))
                .orElseGet(() -> {
                    log.info(
                            "No tenant identified for the received request. {}, {} and {} are empty. Falling back to {}",
                            X_FORWARDED_HOST, HOST, REQUEST_SERVER_NAME, PRIMARY_TENANT_CODE);
                    return PRIMARY_TENANT_CODE;
                });

        log.info("TenantCode: " + tenantCode);
        return tenantCode;
    }

    private Optional<String> getTenantCodeFromConfig(String searchInputName, String search) {

        if (StringUtils.isBlank(search)) {
            return Optional.empty();
        }

        return tenantConfigs.stream().filter(t -> getFqdnTenantNames(t).contains(search)).findFirst()
                .map(TenantConfigDTO::getTenantCode)
                .or(() -> {
                    log.info(
                            "No tenant identified for the received request. {} = '{}'. Falling back to {}",
                            searchInputName, search, PRIMARY_TENANT_CODE);
                    return Optional.of(PRIMARY_TENANT_CODE);
                });
    }

    private List<String> getFqdnTenantNames(TenantConfigDTO tenant) {
        String[] fqdns = tenant.getFqdns().replaceAll("\\s", "").split(",");
        return Arrays.asList(fqdns);
    }
}
