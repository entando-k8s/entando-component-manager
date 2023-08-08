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
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private static final String PRIMARY_TENANT_CODE = "primary";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String HOST = "Host";

    private final List<TenantConfigurationDTO> tenantConfigs;

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

        String tenantCode = PRIMARY_TENANT_CODE;

        if (! CollectionUtils.isEmpty(tenantConfigs)) {
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
            }
        }
        log.info("TenantCode: " + tenantCode);
        return tenantCode;
    }

    private Optional<String> getTenantCodeFromConfig(String search) {
        Optional<TenantConfigurationDTO> tenant = tenantConfigs.stream().filter(
                t -> getFqdnTenantNames(t).contains(search)).findFirst();
        return tenant.map(TenantConfigurationDTO::getTenantCode);
    }

    private List<String> getFqdnTenantNames(TenantConfigurationDTO tenant) {
        String[] fqdns = tenant.getFqdns().replaceAll("\\s", "").split(",");
        return Arrays.asList(fqdns);
    }
}
