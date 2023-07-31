package org.entando.kubernetes.config.tenant;

import liquibase.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Order(1)
@DependsOn("tenantConfig")
public class TenantFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    private static final String PRIMARY_TENANT_CODE = "primary";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String HOST = "Host";

    private final List<TenantConfigDTO> tenantConfigs;

    @Autowired
    public TenantFilter(List<TenantConfigDTO> tenantConfigs) {
        this.tenantConfigs = tenantConfigs;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String domain = UrlUtils.fetchServer(req);

        String xForwardedHost = req.getHeader(X_FORWARDED_HOST);
        String host = req.getHeader(HOST);
        String serverName = req.getServerName();

        String tenantCode = this.getTenantCode(xForwardedHost, host, serverName);
        if (StringUtils.isNotEmpty(tenantCode) && ! tenantCode.equals(PRIMARY_TENANT_CODE)) {
            logger.debug("Tenant code {}", tenantCode);
            TenantContextManager.setTenantCode(tenantCode);
        }

//        try {
            chain.doFilter(request, response);
//        } finally {
//            TenantContext.clear();
//        }
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
        logger.info("TenantCode: " + tenantCode);
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
