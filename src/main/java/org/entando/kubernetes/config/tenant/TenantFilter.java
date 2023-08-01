package org.entando.kubernetes.config.tenant;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private static final String PRIMARY_TENANT_CODE = "primary";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String HOST = "Host";

    private final List<TenantConfigDTO> tenantConfigs;

    private final TenantContextHolder holder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        TenantContext threadLocalContext = new TenantContext();

        String headerXForwardedHost = request.getHeader(X_FORWARDED_HOST);
        String headerHost = request.getHeader(HOST);
        String headerServerName = request.getServerName();

        String tenantCode = this.getTenantCode(headerXForwardedHost, headerHost, headerServerName);

        threadLocalContext.setTenantCode(tenantCode);
        holder.set(threadLocalContext);

        try {
            filterChain.doFilter(request, response);
        } finally {
            holder.remove();
            log.info("Remove custom context from thread local.");
        }
    }

    public String getTenantCode(final String headerXForwardedHost,
            final String headerHost,
            final String servletRequestServerName) {

        String tenantCode = PRIMARY_TENANT_CODE;

        if (tenantConfigs == null || tenantConfigs.isEmpty()) {
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
            }
        }
        log.info("TenantCode: " + tenantCode);
        return tenantCode;
    }

    private Optional<String> getTenantCodeFromConfig(String search) {
        Optional<TenantConfigDTO> tenant = tenantConfigs.stream().filter(
                t -> getFqdnTenantNames(t).contains(search)).findFirst();
        return tenant.map(TenantConfigDTO::getTenantCode);
    }

    private List<String> getFqdnTenantNames(TenantConfigDTO tenant) {
        String[] fqdns = tenant.getFqdns().replaceAll("\\s", "").split(",");
        return Arrays.asList(fqdns);
    }
}
