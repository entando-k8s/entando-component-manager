package org.entando.kubernetes.config.tenant;

import static com.google.common.net.HttpHeaders.HOST;
import static com.google.common.net.HttpHeaders.X_FORWARDED_HOST;

import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String X_ENTANDO_TENANTCODE = " X-ENTANDO-TENANTCODE";
    private final List<TenantConfigDTO> tenantConfigs;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String headerXEntandoTenantCode = request.getHeader(X_ENTANDO_TENANTCODE);
        String headerXForwardedHost = request.getHeader(X_FORWARDED_HOST);
        String headerHost = request.getHeader(HOST);
        String headerServerName = request.getServerName();

        String tenantCode = TenantFilterUtils.fetchTenantCode(tenantConfigs, headerXEntandoTenantCode, headerXForwardedHost,
                headerHost, headerServerName);

        TenantContextHolder.setCurrentTenantCode(tenantCode);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.destroy();
            log.info("Remove custom context from thread local.");
        }
    }

}
