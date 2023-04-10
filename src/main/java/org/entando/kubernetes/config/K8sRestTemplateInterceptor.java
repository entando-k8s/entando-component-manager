package org.entando.kubernetes.config;

import java.io.IOException;
import org.apache.http.HttpHeaders;
import org.entando.kubernetes.client.k8ssvc.FromFileTokenProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class K8sRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private final FromFileTokenProvider tokenProvider;

    public K8sRestTemplateInterceptor(FromFileTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokenProvider.getAccessToken().getTokenValue());
        return execution.execute(request, body);
    }

}
