package org.entando.kubernetes.client.request;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.TenantFilter;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class RestTemplateHeaderTenantCodeInterceptor implements ClientHttpRequestInterceptor {


    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        String tenantCode = TenantContextHolder.getCurrentTenantCode();
        log.debug("add headers interceptor to request with tenantCode:'{}'", tenantCode);
        request.getHeaders().add(TenantFilter.X_ENTANDO_TENANTCODE, tenantCode);
        return execution.execute(request, body);
    }
}
